/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb;

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

import io.ceresdb.common.Display;
import io.ceresdb.common.Endpoint;
import io.ceresdb.common.Lifecycle;
import io.ceresdb.common.VisibleForTest;
import io.ceresdb.common.util.Clock;
import io.ceresdb.common.util.MetricsUtil;
import io.ceresdb.common.util.Requires;
import io.ceresdb.common.util.SerializingExecutor;
import io.ceresdb.common.util.Spines;
import io.ceresdb.common.util.Strings;
import io.ceresdb.errors.StreamException;
import io.ceresdb.limit.LimitedPolicy;
import io.ceresdb.limit.WriteLimiter;
import io.ceresdb.models.Err;
import io.ceresdb.models.Point;
import io.ceresdb.models.RequestContext;
import io.ceresdb.models.Result;
import io.ceresdb.models.Value;
import io.ceresdb.models.WriteOk;
import io.ceresdb.models.WriteRequest;
import io.ceresdb.options.WriteOptions;
import io.ceresdb.proto.internal.Storage;
import io.ceresdb.rpc.Context;
import io.ceresdb.rpc.Observer;
import io.ceresdb.util.StreamWriteBuf;
import io.ceresdb.util.Utils;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.google.common.collect.Lists;

/**
 * Default Write API impl.
 *
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
        this.writeLimiter = new DefaultWriteLimiter(this.opts.getMaxInFlightWritePoints(),
                this.opts.getLimitedPolicy());
        return true;
    }

    @Override
    public void shutdownGracefully() {
        // NO-OP
    }

    @Override
    public CompletableFuture<Result<WriteOk, Err>> write(final WriteRequest req, final Context ctx) {
        req.setReqCtx(attachRequestCtx(req.getReqCtx()));

        Requires.requireTrue(Strings.isNotBlank(req.getReqCtx().getDatabase()), "No database selected");
        Requires.requireNonNull(req.getPoints(), "Null.data");

        final long startCall = Clock.defaultClock().getTick();
        return this.writeLimiter.acquireAndDo(req.getPoints(),
                () -> write0(req.getReqCtx(), req.getPoints(), ctx, 0).whenCompleteAsync((r, e) -> {
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
    public StreamWriteBuf<Point, WriteOk> streamWrite(final RequestContext reqCtx, final String table,
                                                      final Context ctx) {
        final RequestContext finalReqCtx = attachRequestCtx(reqCtx);

        Requires.requireTrue(Strings.isNotBlank(finalReqCtx.getDatabase()), "No database selected");
        Requires.requireTrue(Strings.isNotBlank(table), "Blank.table");

        final CompletableFuture<WriteOk> respFuture = new CompletableFuture<>();

        return this.routerClient.routeFor(reqCtx, Collections.singleton(table))
                .thenApply(routes -> routes.values().stream().findFirst().orElseGet(() -> Route.invalid(table)))
                .thenApply(route -> streamWriteTo(route, finalReqCtx, ctx, Utils.toUnaryObserver(respFuture)))
                .thenApply(reqObserver -> new StreamWriteBuf<Point, WriteOk>() {

                    private final List<Point> buf = Spines.newBuf();

                    @Override
                    public StreamWriteBuf<Point, WriteOk> write(final Point val) {
                        this.buf.add(val);
                        return this;
                    }

                    @Override
                    public StreamWriteBuf<Point, WriteOk> write(final Collection<Point> c) {
                        this.buf.addAll(c);
                        return this;
                    }

                    @Override
                    public StreamWriteBuf<Point, WriteOk> flush() {
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
                    public StreamWriteBuf<Point, WriteOk> writeAndFlush(final Collection<Point> c) {
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

    private RequestContext attachRequestCtx(RequestContext reqCtx) {
        if (reqCtx == null) {
            reqCtx = new RequestContext();
        }
        if (Strings.isNullOrEmpty(reqCtx.getDatabase())) {
            reqCtx.setDatabase(this.opts.getDatabase());
        }
        return reqCtx;
    }

    private CompletableFuture<Result<WriteOk, Err>> write0(final RequestContext reqCtx, final List<Point> data, //
                                                           final Context ctx, //
                                                           final int retries) {
        InnerMetrics.writeByRetries(retries).mark();

        final Set<String> tables = data.stream() //
                .map(Point::getTable) //
                .collect(Collectors.toSet());

        InnerMetrics.metricsNumPerWrite().update(tables.size());

        // 1. Get routes
        return this.routerClient.routeFor(reqCtx, tables)
                // 2. Split data by route info and write to DB
                .thenComposeAsync(routes -> Utils.splitDataByRoute(data, routes).entrySet().stream()
                        // Write to database
                        .map(e -> writeTo(e.getKey(), reqCtx, e.getValue(), ctx.copy(), retries))
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
                            .map(Point::getTable) //
                            .collect(Collectors.toSet());

                    // Should retry
                    final List<Point> pointsToRetry = err.stream() //
                            .filter(Utils::shouldRetry) //
                            .flatMap(e -> e.getFailedWrites().stream()) //
                            .collect(Collectors.toList());

                    // Should not retry
                    final Optional<Err> noRetryErr = err.stream() //
                            .filter(Utils::shouldNotRetry) //
                            .reduce(Err::combine);

                    // Async refresh route info
                    final CompletableFuture<Result<WriteOk, Err>> rwf = this.routerClient
                            .routeRefreshFor(reqCtx, toRefresh)
                            // Even for some data that does not require a refresh of the routing table,
                            // we still wait until the routing table is flushed successfully before
                            // retrying it, in order to give the server a break.
                            .thenComposeAsync(routes -> write0(reqCtx, pointsToRetry, ctx, retries + 1),
                                    this.asyncPool);

                    return noRetryErr.isPresent() ?
                            rwf.thenApplyAsync(ret -> Utils.combineResult(noRetryErr.get().mapToResult(), ret),
                                    this.asyncPool) :
                            rwf.thenApplyAsync(ret -> Utils.combineResult(err.getSubOk().mapToResult(), ret),
                                    this.asyncPool);
                }, this.asyncPool);
    }

    private CompletableFuture<Result<WriteOk, Err>> writeTo(final Endpoint endpoint, //
                                                            final RequestContext reqCtx, //
                                                            final List<Point> data, //
                                                            final Context ctx, //
                                                            final int retries) {
        // The cost is worth it
        final int rowCount = data.size();
        final int maxWriteSize = this.opts.getMaxWriteSize();
        if (rowCount <= maxWriteSize) {
            return writeTo0(endpoint, reqCtx, data, ctx, retries);
        }

        final Stream.Builder<CompletableFuture<Result<WriteOk, Err>>> fs = Stream.builder();

        for (List<Point> part : Lists.partition(data, maxWriteSize)) {
            fs.add(writeTo0(endpoint, reqCtx, part, ctx.copy(), retries));
        }

        return fs.build() //
                .reduce((f1, f2) -> f1.thenCombineAsync(f2, Utils::combineResult, this.asyncPool)) //
                .orElse(Utils.completedCf(WriteOk.emptyOk().mapToResult()));
    }

    private CompletableFuture<Result<WriteOk, Err>> writeTo0(final Endpoint endpoint, //
                                                             final RequestContext reqCtx, //
                                                             final List<Point> data, //
                                                             final Context ctx, //
                                                             final int retries) {
        final CompletableFuture<Storage.WriteResponse> wrf = this.routerClient.invoke(endpoint, //
                toWriteRequestObj(reqCtx, data.stream()), //
                ctx.with("retries", retries) // server can use this in metrics
        );

        return wrf.thenApplyAsync(resp -> Utils.toResult(resp, endpoint, data), this.asyncPool);
    }

    private Observer<Stream<Point>> streamWriteTo(final Route route, //
                                                  final RequestContext reqCtx, //
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

        return new Observer<Stream<Point>>() {

            private final String table = route.getTable();

            @Override
            public void onNext(final Stream<Point> value) {
                final Stream<Point> data = value.filter(point -> {
                    if (this.table.equals(point.getTable())) {
                        return true;
                    }
                    throw new StreamException(
                            String.format("Invalid table %s, only can write %s.", point.getTable(), this.table));
                });

                rpcObs.onNext(toWriteRequestObj(reqCtx, data));
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
        String                                        table;
        Map<String, Storage.WriteSeriesEntry.Builder> seriesBuilders;
        NameDict                                      tagDict;
        NameDict                                      fieldDict;

        public WriteTuple3(String table) {
            this.table = table;
            this.seriesBuilders = new HashMap<>();
            this.tagDict = new NameDict();
            this.fieldDict = new NameDict();
        }

        public Map<String, Storage.WriteSeriesEntry.Builder> getSeriesBuilders() {
            return seriesBuilders;
        }

        public NameDict getTagDict() {
            return tagDict;
        }

        public NameDict getFieldDict() {
            return fieldDict;
        }

        public Storage.WriteTableRequest build() {
            Storage.WriteTableRequest.Builder writeTableRequest = Storage.WriteTableRequest.newBuilder()
                    .setTable(table);

            seriesBuilders.forEach((key, builder) -> {
                writeTableRequest.addEntries(builder.build());
            });

            return writeTableRequest //
                    .addAllTagNames(this.tagDict.toOrdered()) //
                    .addAllFieldNames(this.fieldDict.toOrdered()) //
                    .build();
        }
    }

    @VisibleForTest
    public Storage.WriteRequest toWriteRequestObj(final RequestContext reqCtx, final Stream<Point> data) {
        final Storage.WriteRequest.Builder writeRequestBuilder = Storage.WriteRequest.newBuilder();
        final Map<String, WriteTuple3> tuple3s = new HashMap<>();

        data.forEach(point -> {
            final String table = point.getTable();
            final WriteTuple3 tp3 = tuple3s.computeIfAbsent(table, WriteTuple3::new);

            final NameDict tagDict = tp3.getTagDict();
            point.getTags().forEach((tagK, tagV) -> {
                tagDict.insert(tagK);
            });
            StringBuffer seriesKeyBuffer = new StringBuffer();
            tagDict.toOrdered().forEach((tagK) -> {
                Value tagV = point.getTags().get(tagK);
                if (!Value.isNull(tagV)) {
                    seriesKeyBuffer.append(tagV.getValue().toString());
                }
            });
            Storage.WriteSeriesEntry.Builder seriesEntryBuilder = tp3.getSeriesBuilders()
                    .computeIfAbsent(seriesKeyBuffer.toString(), seriesKey -> {
                        final Storage.WriteSeriesEntry.Builder seBuilder = Storage.WriteSeriesEntry.newBuilder();
                        point.getTags().forEach((tagK, tagV) -> {
                            if (Value.isNull(tagV)) {
                                return;
                            }
                            final Storage.Tag.Builder tBui = Storage.Tag.newBuilder().setNameIndex(tagDict.insert(tagK))
                                    .setValue(Utils.toProtoValue(tagV));
                            seBuilder.addTags(tBui.build());
                        });
                        return seBuilder;
                    });

            final NameDict fieldDict = tp3.getFieldDict();
            final Storage.FieldGroup.Builder fgBui = Storage.FieldGroup.newBuilder().setTimestamp(point.getTimestamp());
            point.getFields().forEach((fieldK, fieldV) -> {
                if (Value.isNull(fieldV)) {
                    return;
                }
                final Storage.Field.Builder fBui = Storage.Field.newBuilder().setNameIndex(fieldDict.insert(fieldK))
                        .setValue(Utils.toProtoValue(fieldV));
                fgBui.addFields(fBui.build());
            });
            seriesEntryBuilder.addFieldGroups(fgBui.build());
        });

        Storage.RequestContext.Builder ctxBuilder = Storage.RequestContext.newBuilder();
        ctxBuilder.setDatabase(reqCtx.getDatabase());
        writeRequestBuilder.setContext(ctxBuilder.build());

        tuple3s.values().forEach(tp3 -> writeRequestBuilder.addTableRequests(tp3.build()));

        return writeRequestBuilder.build();
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
        public int calculatePermits(final List<Point> in) {
            return in == null ? 0 : in.size();
        }

        @Override
        public Result<WriteOk, Err> rejected(final List<Point> in, final RejectedState state) {
            final String errMsg = String.format(
                    "Write limited by client, acquirePermits=%d, maxPermits=%d, availablePermits=%d.", //
                    state.acquirePermits(), //
                    state.maxPermits(), //
                    state.availablePermits());
            return Result.err(Err.writeErr(Result.FLOW_CONTROL, errMsg, null, in));
        }
    }
}
