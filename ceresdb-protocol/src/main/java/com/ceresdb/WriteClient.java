/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ceresdb;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceresdb.common.Display;
import com.ceresdb.common.Endpoint;
import com.ceresdb.common.Lifecycle;
import com.ceresdb.common.VisibleForTest;
import com.ceresdb.common.util.Clock;
import com.ceresdb.common.util.MetricsUtil;
import com.ceresdb.common.util.Requires;
import com.ceresdb.common.util.SerializingExecutor;
import com.ceresdb.common.util.Spines;
import com.ceresdb.common.util.Strings;
import com.ceresdb.errors.StreamException;
import com.ceresdb.models.Err;
import com.ceresdb.models.Result;
import com.ceresdb.models.Rows;
import com.ceresdb.models.Value;
import com.ceresdb.models.WriteOk;
import com.ceresdb.options.WriteOptions;
import com.ceresdb.proto.Storage;
import com.ceresdb.rpc.Context;
import com.ceresdb.rpc.Observer;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;

/**
 * Default Write API impl.
 *
 * @author jiachun.fjc
 */
public class WriteClient implements Write, Lifecycle<WriteOptions>, Display {

    private static final Logger LOG = LoggerFactory.getLogger(WriteClient.class);

    private WriteOptions opts;
    private RouterClient routerClient;
    private Executor     asyncPool;
    private WriteLimiter writeLimiter;

    static final class InnerMetrics {
        static final Histogram WRITE_ROWS_SUCCESS    = MetricsUtil.histogram("write_rows_success_num");
        static final Histogram WRITE_ROWS_FAILED     = MetricsUtil.histogram("write_rows_failed_num");
        static final Histogram METRICS_NUM_PER_WRITE = MetricsUtil.histogram("metrics_num_per_write");
        static final Meter     WRITE_FAILED          = MetricsUtil.meter("write_failed");
        static final Meter     WRITE_QPS             = MetricsUtil.meter("write_qps");

        static Histogram writeRowsSuccess() {
            return WRITE_ROWS_SUCCESS;
        }

        static Histogram writeRowsFailed() {
            return WRITE_ROWS_FAILED;
        }

        static Histogram metricsNumPerWrite() {
            return METRICS_NUM_PER_WRITE;
        }

        static Meter writeFailed() {
            return WRITE_FAILED;
        }

        static Meter writeQps() {
            return WRITE_QPS;
        }

        static Meter writeByRetries(final int retries) {
            // more than 3 retries are classified as the same metric
            return MetricsUtil.meter("write_by_retries", Math.min(3, retries));
        }
    }

    @Override
    public boolean init(final WriteOptions opts) {
        this.opts = Requires.requireNonNull(opts, "WriteClient.opts");
        this.routerClient = this.opts.getRoutedClient();
        final Executor pool = this.opts.getAsyncPool();
        this.asyncPool = pool != null ? pool : new SerializingExecutor("write_client");
        this.writeLimiter = new DefaultWriteLimiter(this.opts.getMaxInFlightWriteRows(), this.opts.getLimitedPolicy());
        return true;
    }

    @Override
    public void shutdownGracefully() {
        // NO-OP
    }

    @Override
    public CompletableFuture<Result<WriteOk, Err>> write(final Collection<Rows> data, final Context ctx) {
        Requires.requireNonNull(data, "Null.data");
        final long startCall = Clock.defaultClock().getTick();
        return this.writeLimiter.acquireAndDo(data, () -> write0(data, ctx, 0).whenCompleteAsync((r, e) -> {
            InnerMetrics.writeQps().mark();
            if (r != null) {
                if (Utils.isRwLogging()) {
                    LOG.info("Write to {}, duration={} ms, result={}.", Utils.DB_NAME,
                            Clock.defaultClock().duration(startCall), r);
                }
                if (r.isOk()) {
                    final WriteOk ok = r.getOk();
                    InnerMetrics.writeRowsSuccess().update(ok.getSuccess());
                    InnerMetrics.writeRowsFailed().update(ok.getFailed());
                    return;
                }
            }
            InnerMetrics.writeFailed().mark();
        }, this.asyncPool));
    }

    @Override
    public StreamWriteBuf<Rows, WriteOk> streamWrite(final String metric, final Context ctx) {
        Requires.requireTrue(Strings.isNotBlank(metric), "Blank.metric");

        final CompletableFuture<WriteOk> respFuture = new CompletableFuture<>();

        return this.routerClient.routeFor(Collections.singleton(metric))
                .thenApply(routes -> routes.values().stream().findFirst().orElseGet(() -> Route.invalid(metric)))
                .thenApply(route -> streamWriteTo(route, ctx, Utils.toUnaryObserver(respFuture)))
                .thenApply(reqObserver -> new StreamWriteBuf<Rows, WriteOk>() {

                    private final Collection<Rows> buf = Spines.newBuf();

                    @Override
                    public StreamWriteBuf<Rows, WriteOk> write(final Rows val) {
                        this.buf.add(val);
                        return this;
                    }

                    @Override
                    public StreamWriteBuf<Rows, WriteOk> write(final Collection<Rows> c) {
                        this.buf.addAll(c);
                        return this;
                    }

                    @Override
                    public StreamWriteBuf<Rows, WriteOk> flush() {
                        if (respFuture.isCompletedExceptionally()) {
                            respFuture.getNow(null); // throw the exception now
                        }
                        if (!this.buf.isEmpty()) {
                            reqObserver.onNext(this.buf.stream());
                            this.buf.clear();
                        }
                        return this;
                    }

                    @Override
                    public StreamWriteBuf<Rows, WriteOk> writeAndFlush(final Collection<Rows> c) {
                        flush(); // flush the previous write
                        reqObserver.onNext(c.stream());
                        return this;
                    }

                    @Override
                    public CompletableFuture<WriteOk> completed() {
                        flush();
                        reqObserver.onCompleted();
                        return respFuture;
                    }
                }).join();
    }

    private CompletableFuture<Result<WriteOk, Err>> write0(final Collection<Rows> data, //
                                                           final Context ctx, //
                                                           final int retries) {
        InnerMetrics.writeByRetries(retries).mark();

        final Set<String> metrics = data.stream() //
                .map(Rows::getMetric) //
                .collect(Collectors.toSet());

        InnerMetrics.metricsNumPerWrite().update(metrics.size());

        // 1. Get routes
        return this.routerClient.routeFor(metrics)
                // 2. Split data by route info and write to DB
                .thenComposeAsync(routes -> Utils.splitDataByRoute(data, routes).entrySet().stream()
                        // Write to database
                        .map(e -> writeTo(e.getKey(), e.getValue(), ctx.copy(), retries))
                        // Reduce and combine write result
                        .reduce((f1, f2) -> f1.thenCombineAsync(f2, Utils::combineResult, this.asyncPool))
                        .orElse(Utils.completedCf(WriteOk.emptyOk().mapToResult())), this.asyncPool)
                // 3. If failed, refresh route info and retry on INVALID_ROUTE
                .thenComposeAsync(r -> {
                    if (r.isOk()) {
                        LOG.debug("Success to write to {}, ok={}.", Utils.DB_NAME, r.getOk());
                        return Utils.completedCf(r);
                    }

                    final Err err = r.getErr();
                    LOG.warn("Failed to write to {}, retries={}, err={}.", Utils.DB_NAME, retries, err);
                    if (retries + 1 > this.opts.getMaxRetries()) {
                        LOG.error("Retried {} times still failed.", retries);
                        return Utils.completedCf(r);
                    }

                    // Should refresh route table
                    final Set<String> toRefresh = err.stream() //
                            .filter(Utils::shouldRefreshRouteTable) //
                            .flatMap(e -> e.getFailedWrites().stream()) //
                            .map(Rows::getMetric) //
                            .collect(Collectors.toSet());

                    // Should retries
                    final List<Rows> rowsToRetry = err.stream() //
                            .filter(Utils::shouldRetry) //
                            .flatMap(e -> e.getFailedWrites().stream()) //
                            .collect(Collectors.toList());

                    // Should not retries
                    final Optional<Err> noRetryErr = err.stream() //
                            .filter(Utils::shouldNotRetry) //
                            .reduce(Err::combine);

                    // Async refresh route info
                    final CompletableFuture<Result<WriteOk, Err>> rwf = this.routerClient.routeRefreshFor(toRefresh)
                            // Even for some data that does not require a refresh of the routing table,
                            // we still wait until the routing table is flushed successfully before
                            // retrying it, in order to give the server a break.
                            .thenComposeAsync(routes -> write0(rowsToRetry, ctx, retries + 1), this.asyncPool);

                    return noRetryErr.isPresent() ?
                            rwf.thenApplyAsync(ret -> Utils.combineResult(noRetryErr.get().mapToResult(), ret),
                                    this.asyncPool) :
                            rwf.thenApplyAsync(ret -> Utils.combineResult(err.getSubOk().mapToResult(), ret),
                                    this.asyncPool);
                }, this.asyncPool);
    }

    private CompletableFuture<Result<WriteOk, Err>> writeTo(final Endpoint endpoint, //
                                                            final Collection<Rows> data, //
                                                            final Context ctx, //
                                                            final int retries) {
        // The cost is worth it
        final int rowCount = data.stream() //
                .map(Rows::getRowCount) //
                .reduce(0, Integer::sum);
        final int maxWriteSize = this.opts.getMaxWriteSize();
        if (rowCount <= maxWriteSize) {
            return writeTo0(endpoint, data, ctx, retries);
        }

        final Stream.Builder<CompletableFuture<Result<WriteOk, Err>>> fs = Stream.builder();
        final PartBuf partBuf = new PartBuf();
        for (final Rows rs : data) {
            final int rc = rs.getRowCount();
            if (partBuf.isNotEmpty() && partBuf.preAdd(rc) > maxWriteSize) {
                fs.add(writeTo0(endpoint, partBuf.collectAndReset(), ctx.copy(), retries));
            }
            partBuf.add(rs);
        }
        if (partBuf.isNotEmpty()) {
            fs.add(writeTo0(endpoint, partBuf.collectAndReset(), ctx.copy(), retries));
        }

        return fs.build() //
                .reduce((f1, f2) -> f1.thenCombineAsync(f2, Utils::combineResult, this.asyncPool)) //
                .orElse(Utils.completedCf(WriteOk.emptyOk().mapToResult()));
    }

    private static class PartBuf {
        private Collection<Rows> buf;
        private int              count;

        public void add(final Rows rs) {
            if (this.buf == null) {
                this.buf = Spines.newBuf();
            }
            this.buf.add(rs);
            this.count += rs.getRowCount();
        }

        public int preAdd(final int c) {
            return this.count + c;
        }

        public boolean isNotEmpty() {
            return this.count > 0;
        }

        public Collection<Rows> collectAndReset() {
            final Collection<Rows> ret = this.buf;
            // Cannot reuse the buf, outside will reference it until the response arrives.
            this.buf = null;
            this.count = 0;
            return ret;
        }
    }

    private CompletableFuture<Result<WriteOk, Err>> writeTo0(final Endpoint endpoint, //
                                                             final Collection<Rows> data, //
                                                             final Context ctx, //
                                                             final int retries) {
        final CompletableFuture<Storage.WriteResponse> wrf = this.routerClient.invoke(endpoint, //
                toWriteRequestObj(data.stream()), //
                ctx.with("retries", retries) // server can use this in metrics
        );

        return wrf.thenApplyAsync(resp -> Utils.toResult(resp, endpoint, data), this.asyncPool);
    }

    private Observer<Stream<Rows>> streamWriteTo(final Route route, //
                                                 final Context ctx, //
                                                 final Observer<WriteOk> respObserver) {
        final Observer<Storage.WriteRequest> rpcObs = this.routerClient.invokeClientStreaming(route.getEndpoint(), //
                Storage.WriteRequest.getDefaultInstance(), //
                ctx, //
                new Observer<Storage.WriteResponse>() {

                    @Override
                    public void onNext(final Storage.WriteResponse value) {
                        final Result<WriteOk, Err> ret = Utils.toResult(value, route.getEndpoint(), null);
                        if (ret.isOk()) {
                            respObserver.onNext(ret.getOk());
                        } else {
                            respObserver.onError(new StreamException("Failed to do stream write: " + ret.getErr()));
                        }
                    }

                    @Override
                    public void onError(final Throwable err) {
                        respObserver.onError(err);
                    }

                    @Override
                    public void onCompleted() {
                        respObserver.onCompleted();
                    }
                });

        return new Observer<Stream<Rows>>() {

            private final String metric = route.getMetric();

            @Override
            public void onNext(final Stream<Rows> value) {
                final Stream<Rows> data = value.filter(rs -> {
                    if (this.metric.equals(rs.getMetric())) {
                        return true;
                    }
                    throw new StreamException(
                            String.format("Invalid metric %s, only can write %s.", rs.getMetric(), this.metric));
                });

                rpcObs.onNext(toWriteRequestObj(data));
            }

            @Override
            public void onError(final Throwable err) {
                rpcObs.onError(err);
            }

            @Override
            public void onCompleted() {
                rpcObs.onCompleted();
            }
        };
    }

    private static class NameDict {
        private final Map<String, Integer> nameIndexes = new HashMap<>();
        private int                        index       = 0;

        public int insert(final String name) {
            return this.nameIndexes.computeIfAbsent(name, s -> this.index++);
        }

        public Iterable<String> toOrdered() {
            final String[] arr = new String[this.index];
            this.nameIndexes.forEach((name, i) -> arr[i] = name);

            return () -> new Iterator<String>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return this.index < arr.length;
                }

                @Override
                public String next() {
                    return arr[this.index++];
                }
            };
        }
    }

    private static class WriteTuple3 {
        Storage.WriteMetric.Builder wmcBui;
        NameDict                    tagDict;
        NameDict                    fieldDict;

        public WriteTuple3(String metric) {
            this.wmcBui = Storage.WriteMetric.newBuilder().setMetric(metric);
            this.tagDict = new NameDict();
            this.fieldDict = new NameDict();
        }

        public Storage.WriteMetric.Builder getWmcBui() {
            return wmcBui;
        }

        public NameDict getTagDict() {
            return tagDict;
        }

        public NameDict getFieldDict() {
            return fieldDict;
        }

        public Storage.WriteMetric build() {
            return this.wmcBui //
                    .addAllTagNames(this.tagDict.toOrdered()) //
                    .addAllFieldNames(this.fieldDict.toOrdered()) //
                    .build();
        }
    }

    @VisibleForTest
    public Storage.WriteRequest toWriteRequestObj(final Stream<Rows> data) {
        final Storage.WriteRequest.Builder wrBui = Storage.WriteRequest.newBuilder();
        final Map<String, WriteTuple3> tuple3s = new HashMap<>();

        data.forEach(rs -> {
            final String metric = rs.getMetric();
            final WriteTuple3 tp3 = tuple3s.computeIfAbsent(metric, WriteTuple3::new);
            final Storage.WriteEntry.Builder weyBui = Storage.WriteEntry.newBuilder();

            final NameDict tagDict = tp3.getTagDict();
            rs.getSeries().getTags().forEach((tagK, tagV) -> {
                if (Value.isNull(tagV)) {
                    return;
                }
                final Storage.Tag.Builder tBui = Storage.Tag.newBuilder().setNameIndex(tagDict.insert(tagK))
                        .setValue(Utils.toProtoValue(tagV));
                weyBui.addTags(tBui.build());
            });

            final NameDict fieldDict = tp3.getFieldDict();
            rs.getFields().forEach((ts, fields) -> {
                final Storage.FieldGroup.Builder fgBui = Storage.FieldGroup.newBuilder().setTimestamp(ts);
                fields.forEach((name, field) -> {
                    if (Value.isNull(field)) {
                        return;
                    }
                    final Storage.Field.Builder fBui = Storage.Field.newBuilder().setNameIndex(fieldDict.insert(name))
                            .setValue(Utils.toProtoValue(field));
                    fgBui.addFields(fBui.build());
                });
                weyBui.addFieldGroups(fgBui.build());
            });

            tp3.getWmcBui().addEntries(weyBui.build());
        });

        tuple3s.values().forEach(tp3 -> wrBui.addMetrics(tp3.build()));

        return wrBui.build();
    }

    @Override
    public void display(final Printer out) {
        out.println("--- WriteClient ---") //
                .print("maxRetries=") //
                .println(this.opts.getMaxRetries()) //
                .print("maxWriteSize=") //
                .println(this.opts.getMaxWriteSize()) //
                .print("asyncPool=") //
                .println(this.asyncPool);
    }

    @Override
    public String toString() {
        return "WriteClient{" + //
               "opts=" + opts + //
               ", routerClient=" + routerClient + //
               ", asyncPool=" + asyncPool + //
               '}';
    }

    @VisibleForTest
    static class DefaultWriteLimiter extends WriteLimiter {

        public DefaultWriteLimiter(int maxInFlight, LimitedPolicy policy) {
            super(maxInFlight, policy, "write_limiter_acquire");
        }

        @Override
        public int calculatePermits(final Collection<Rows> in) {
            return in == null ? 0 : in.stream().map(Rows::getRowCount).reduce(0, Integer::sum);
        }

        @Override
        public Result<WriteOk, Err> rejected(final Collection<Rows> in, final RejectedState state) {
            final String errMsg = String.format(
                    "Write limited by client, acquirePermits=%d, maxPermits=%d, availablePermits=%d.", //
                    state.acquirePermits(), //
                    state.maxPermits(), //
                    state.availablePermits());
            return Result.err(Err.writeErr(Result.FLOW_CONTROL, errMsg, null, in));
        }
    }
}
