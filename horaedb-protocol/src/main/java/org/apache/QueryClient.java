/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.apache.horaedb.limit.LimitedPolicy;
import org.apache.horaedb.limit.QueryLimiter;
import org.apache.horaedb.models.RequestContext;
import io.ceresdb.proto.internal.Storage;
import org.apache.horaedb.common.Display;
import org.apache.horaedb.common.Endpoint;
import org.apache.horaedb.common.Lifecycle;
import org.apache.horaedb.common.VisibleForTest;
import org.apache.horaedb.common.parser.SqlParser;
import org.apache.horaedb.common.parser.SqlParserFactoryProvider;
import org.apache.horaedb.common.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.horaedb.errors.StreamException;
import org.apache.horaedb.models.Err;
import org.apache.horaedb.models.SqlQueryOk;
import org.apache.horaedb.models.SqlQueryRequest;
import org.apache.horaedb.models.Result;
import org.apache.horaedb.options.QueryOptions;
import org.apache.horaedb.rpc.Context;
import org.apache.horaedb.rpc.Observer;
import org.apache.horaedb.util.Utils;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;

/**
 * Default Query API impl.
 *
 */
public class QueryClient implements Query, Lifecycle<QueryOptions>, Display {

    private static final Logger LOG = LoggerFactory.getLogger(QueryClient.class);

    private QueryOptions opts;
    private RouterClient routerClient;
    private Executor     asyncPool;
    private QueryLimiter queryLimiter;

    static final class InnerMetrics {
        static final Histogram READ_ROWS_COUNT = MetricsUtil.histogram("read_rows_count");
        static final Meter     READ_FAILED     = MetricsUtil.meter("read_failed");
        static final Meter     READ_QPS        = MetricsUtil.meter("read_qps");

        static Histogram readRowsCount() {
            return READ_ROWS_COUNT;
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
    public CompletableFuture<Result<SqlQueryOk, Err>> sqlQuery(final SqlQueryRequest req, final Context ctx) {
        req.setReqCtx(attachRequestCtx(req.getReqCtx()));

        Requires.requireNonNull(req, "Null.request");
        Requires.requireTrue(Strings.isNotBlank(req.getReqCtx().getDatabase()), "No database selected");

        final long startCall = Clock.defaultClock().getTick();
        setMetricsIfAbsent(req);
        return this.queryLimiter.acquireAndDo(req, () -> query0(req, ctx, 0).whenCompleteAsync((r, e) -> {
            InnerMetrics.readQps().mark();
            if (r != null) {
                final int rowCount = r.mapOr(0, SqlQueryOk::getRowCount);
                InnerMetrics.readRowsCount().update(rowCount);
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
    public void streamSqlQuery(final SqlQueryRequest req, final Context ctx, final Observer<SqlQueryOk> observer) {
        req.setReqCtx(attachRequestCtx(req.getReqCtx()));

        Requires.requireNonNull(req, "Null.request");
        Requires.requireNonNull(observer, "Null.observer");
        Requires.requireTrue(Strings.isNotBlank(req.getReqCtx().getDatabase()), "No database selected");

        setMetricsIfAbsent(req);

        this.routerClient.routeFor(req.getReqCtx(), req.getTables())
                .thenApply(routes -> routes.values().stream().findAny().orElse(this.routerClient.clusterRoute()))
                .thenAccept(route -> streamQueryFrom(route.getEndpoint(), req, ctx, observer));
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

    private CompletableFuture<Result<SqlQueryOk, Err>> query0(final SqlQueryRequest req, //
                                                              final Context ctx, //
                                                              final int retries) {
        InnerMetrics.readByRetries(retries).mark();

        return this.routerClient.routeFor(req.getReqCtx(), req.getTables()) //
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
                    LOG.warn("Failed to read from {}, err={}.", Utils.DB_NAME, err);

                    // Should refresh route table
                    final Set<String> toRefresh = err.stream() //
                            .filter(Utils::shouldRefreshRouteTable) //
                            .flatMap(e -> e.getFailedTables().stream()) //
                            .collect(Collectors.toSet());
                    this.routerClient.clearRouteCacheBy(toRefresh);

                    // Should not retry
                    if (Utils.shouldNotRetry(err)) {
                        return Utils.completedCf(r);
                    }

                    // Async to refresh route info
                    if (retries > this.opts.getMaxRetries()) {
                        LOG.error("Retried {} times still failed.", retries);
                        return Utils.completedCf(r);
                    }
                    return this.routerClient.routeFor(req.getReqCtx(), toRefresh)
                            .thenComposeAsync(routes -> query0(req, ctx, retries + 1), this.asyncPool);
                }, this.asyncPool);
    }

    private void setMetricsIfAbsent(final SqlQueryRequest req) {
        if (req.getTables() != null && !req.getTables().isEmpty()) {
            return;
        }
        final SqlParser parser = SqlParserFactoryProvider.getSqlParserFactory().getParser(req.getSql());
        req.setTables(parser.tableNames());
    }

    private static final class ErrHandler implements Runnable {

        private final SqlQueryRequest req;

        private ErrHandler(SqlQueryRequest req) {
            this.req = req;
        }

        @Override
        public void run() {
            LOG.error("Fail to query by request: {}.", this.req);
        }
    }

    private CompletableFuture<Result<SqlQueryOk, Err>> queryFrom(final Endpoint endpoint, //
                                                                 final SqlQueryRequest req, //
                                                                 final Context ctx, //
                                                                 final int retries) {
        final Storage.SqlQueryRequest request = Storage.SqlQueryRequest.newBuilder() //
                .setContext(Storage.RequestContext.newBuilder().setDatabase(req.getReqCtx().getDatabase()).build()) //
                .addAllTables(req.getTables()) //
                .setSql(req.getSql()) //
                .build();

        final CompletableFuture<Storage.SqlQueryResponse> qrf = this.routerClient.invoke(endpoint, //
                request, //
                ctx.with("retries", retries) // server can use this in metrics
        );

        return qrf.thenApplyAsync(
                resp -> Utils.toResult(resp, req.getSql(), endpoint, req.getTables(), new ErrHandler(req)),
                this.asyncPool);
    }

    private void streamQueryFrom(final Endpoint endpoint, //
                                 final SqlQueryRequest req, //
                                 final Context ctx, //
                                 final Observer<SqlQueryOk> observer) {
        final Storage.SqlQueryRequest request = Storage.SqlQueryRequest.newBuilder() //
                .setContext(Storage.RequestContext.newBuilder().setDatabase(req.getReqCtx().getDatabase()).build()) //
                .addAllTables(req.getTables()) //
                .setSql(req.getSql()) //
                .build();

        this.routerClient.invokeServerStreaming(endpoint, request, ctx, new Observer<Storage.SqlQueryResponse>() {

            @Override
            public void onNext(final Storage.SqlQueryResponse value) {
                final Result<SqlQueryOk, Err> ret = Utils.toResult(value, req.getSql(), endpoint, req.getTables(),
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
        public int calculatePermits(final SqlQueryRequest request) {
            return 1;
        }

        @Override
        public Result<SqlQueryOk, Err> rejected(final SqlQueryRequest request, final RejectedState state) {
            final String errMsg = String.format(
                    "Query limited by client, acquirePermits=%d, maxPermits=%d, availablePermits=%d.", //
                    state.acquirePermits(), //
                    state.maxPermits(), //
                    state.availablePermits());
            return Result.err(Err.queryErr(Result.FLOW_CONTROL, errMsg, null, request.getSql(), request.getTables()));
        }
    }
}
