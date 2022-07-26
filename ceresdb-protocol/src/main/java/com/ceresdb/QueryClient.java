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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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
import com.ceresdb.errors.StreamException;
import com.ceresdb.models.Err;
import com.ceresdb.models.QueryOk;
import com.ceresdb.models.QueryRequest;
import com.ceresdb.models.Result;
import com.ceresdb.options.QueryOptions;
import com.ceresdb.proto.Storage;
import com.ceresdb.rpc.Context;
import com.ceresdb.rpc.Observer;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;

/**
 * Default Query API impl.
 *
 * @author jiachun.fjc
 */
public class QueryClient implements Query, Lifecycle<QueryOptions>, Display {

    private static final Logger LOG = LoggerFactory.getLogger(QueryClient.class);

    private QueryOptions opts;
    private RouterClient routerClient;
    private Executor     asyncPool;
    private QueryLimiter queryLimiter;

    static final class InnerMetrics {
        static final Histogram READ_ROW_COUNT = MetricsUtil.histogram("read_row_count");
        static final Meter     READ_FAILED    = MetricsUtil.meter("read_failed");
        static final Meter     READ_QPS       = MetricsUtil.meter("read_qps");

        static Histogram readRowCount() {
            return READ_ROW_COUNT;
        }

        static Meter readFailed() {
            return READ_FAILED;
        }

        static Meter readQps() {
            return READ_QPS;
        }

        static Meter readByRetries(final int retries) {
            // more than 3 retries are classified as the same metric
            return MetricsUtil.meter("read_by_retries", Math.min(3, retries));
        }
    }

    @Override
    public boolean init(final QueryOptions opts) {
        this.opts = Requires.requireNonNull(opts, "QueryOptions.opts");
        this.routerClient = this.opts.getRouterClient();
        final Executor pool = this.opts.getAsyncPool();
        this.asyncPool = pool != null ? pool : new SerializingExecutor("query_client");
        this.queryLimiter = new DefaultQueryLimiter(this.opts.getMaxInFlightQueryRequests(),
                this.opts.getLimitedPolicy());
        return true;
    }

    @Override
    public void shutdownGracefully() {
        // NO-OP
    }

    @Override
    public CompletableFuture<Result<QueryOk, Err>> query(final QueryRequest req, final Context ctx) {
        Requires.requireNonNull(req, "Null.request");
        final long startCall = Clock.defaultClock().getTick();
        setMetricsIfAbsent(req);
        return this.queryLimiter.acquireAndDo(req, () -> query0(req, ctx, 0).whenCompleteAsync((r, e) -> {
            InnerMetrics.readQps().mark();
            if (r != null) {
                final int rowCount = r.mapOr(0, QueryOk::getRowCount);
                InnerMetrics.readRowCount().update(rowCount);
                if (Utils.isRwLogging()) {
                    LOG.info("Read from {}, duration={} ms, rowCount={}.", Utils.DB_NAME,
                            Clock.defaultClock().duration(startCall), rowCount);
                }
                if (r.isOk()) {
                    return;
                }
            }
            InnerMetrics.readFailed().mark();
        }, this.asyncPool));
    }

    @Override
    public void streamQuery(final QueryRequest req, final Context ctx, final Observer<QueryOk> observer) {
        Requires.requireNonNull(req, "Null.request");
        Requires.requireNonNull(observer, "Null.observer");

        setMetricsIfAbsent(req);

        this.routerClient.routeFor(req.getMetrics())
                .thenApply(routes -> routes.values().stream().findAny().orElse(this.routerClient.clusterRoute()))
                .thenAccept(route -> streamQueryFrom(route.getEndpoint(), req, ctx, observer));
    }

    private CompletableFuture<Result<QueryOk, Err>> query0(final QueryRequest req, //
                                                           final Context ctx, //
                                                           final int retries) {
        InnerMetrics.readByRetries(retries).mark();

        return this.routerClient.routeFor(req.getMetrics()) //
                .thenApplyAsync(routes -> routes.values() //
                        .stream() //
                        .findAny() // everyone is OK
                        .orElse(this.routerClient.clusterRoute()), this.asyncPool) //
                .thenComposeAsync(route -> queryFrom(route.getEndpoint(), req, ctx, retries), this.asyncPool)
                .thenComposeAsync(r -> {
                    if (r.isOk()) {
                        LOG.debug("Success to read from {}, ok={}.", Utils.DB_NAME, r.getOk());
                        return Utils.completedCf(r);
                    }

                    final Err err = r.getErr();
                    LOG.warn("Failed to read from {}, retries={}, err={}.", Utils.DB_NAME, retries, err);
                    if (retries > this.opts.getMaxRetries()) {
                        LOG.error("Retried {} times still failed.", retries);
                        return Utils.completedCf(r);
                    }

                    // Should refresh route table
                    final Set<String> toRefresh = err.stream() //
                            .filter(Utils::shouldRefreshRouteTable) //
                            .flatMap(e -> e.getFailedMetrics().stream()) //
                            .collect(Collectors.toSet());

                    if (toRefresh.isEmpty()) {
                        return Utils.completedCf(r);
                    }

                    // Async to refresh route info
                    return this.routerClient.routeRefreshFor(toRefresh)
                            .thenComposeAsync(routes -> query0(req, ctx, retries + 1), this.asyncPool);
                }, this.asyncPool);
    }

    private void setMetricsIfAbsent(final QueryRequest req) {
        if (req.getMetrics() != null && !req.getMetrics().isEmpty()) {
            return;
        }
        final MetricParser parser = MetricParserFactoryProvider.getMetricParserFactory().getParser(req.getQl());
        req.setMetrics(parser.metricNames());
    }

    private static final class ErrHandler implements Runnable {

        private final QueryRequest req;

        private ErrHandler(QueryRequest req) {
            this.req = req;
        }

        @Override
        public void run() {
            LOG.error("Fail to query by request: {}.", this.req);
        }
    }

    private CompletableFuture<Result<QueryOk, Err>> queryFrom(final Endpoint endpoint, //
                                                              final QueryRequest req, //
                                                              final Context ctx, //
                                                              final int retries) {
        final Storage.QueryRequest request = Storage.QueryRequest.newBuilder() //
                .addAllMetrics(req.getMetrics()) //
                .setQl(req.getQl()) //
                .build();

        final CompletableFuture<Storage.QueryResponse> qrf = this.routerClient.invoke(endpoint, //
                request, //
                ctx.with("retries", retries) // server can use this in metrics
        );

        return qrf.thenApplyAsync(
                resp -> Utils.toResult(resp, req.getQl(), endpoint, req.getMetrics(), new ErrHandler(req)),
                this.asyncPool);
    }

    private void streamQueryFrom(final Endpoint endpoint, //
                                 final QueryRequest req, //
                                 final Context ctx, //
                                 final Observer<QueryOk> observer) {
        final Storage.QueryRequest request = Storage.QueryRequest.newBuilder() //
                .addAllMetrics(req.getMetrics()) //
                .setQl(req.getQl()) //
                .build();

        this.routerClient.invokeServerStreaming(endpoint, request, ctx, new Observer<Storage.QueryResponse>() {

            @Override
            public void onNext(final Storage.QueryResponse value) {
                final Result<QueryOk, Err> ret = Utils.toResult(value, req.getQl(), endpoint, req.getMetrics(),
                        new ErrHandler(req));
                if (ret.isOk()) {
                    observer.onNext(ret.getOk());
                } else {
                    observer.onError(new StreamException("Failed to do stream query: " + ret.getErr()));
                }
            }

            @Override
            public void onError(final Throwable err) {
                observer.onError(err);
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }

            @Override
            public Executor executor() {
                return observer.executor();
            }
        });
    }

    @Override
    public void display(final Printer out) {
        out.println("--- QueryClient ---") //
                .print("maxRetries=") //
                .println(this.opts.getMaxRetries()) //
                .print("asyncPool=") //
                .println(this.asyncPool);
    }

    @Override
    public String toString() {
        return "QueryClient{" + //
               "opts=" + opts + //
               ", routerClient=" + routerClient + //
               ", asyncPool=" + asyncPool + //
               '}';
    }

    @VisibleForTest
    static class DefaultQueryLimiter extends QueryLimiter {

        public DefaultQueryLimiter(int maxInFlight, LimitedPolicy policy) {
            super(maxInFlight, policy, "query_limiter_acquire");
        }

        @Override
        public int calculatePermits(final QueryRequest request) {
            return 1;
        }

        @Override
        public Result<QueryOk, Err> rejected(final QueryRequest request, final RejectedState state) {
            final String errMsg = String.format(
                    "Query limited by client, acquirePermits=%d, maxPermits=%d, availablePermits=%d.", //
                    state.acquirePermits(), //
                    state.maxPermits(), //
                    state.availablePermits());
            return Result.err(Err.queryErr(Result.FLOW_CONTROL, errMsg, null, request.getQl(), request.getMetrics()));
        }
    }
}
