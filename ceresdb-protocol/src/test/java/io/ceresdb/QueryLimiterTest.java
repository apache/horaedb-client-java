/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import io.ceresdb.errors.LimitedException;
import io.ceresdb.limit.LimitedPolicy;
import io.ceresdb.limit.QueryLimiter;
import io.ceresdb.models.Err;
import io.ceresdb.models.SqlQueryOk;
import io.ceresdb.models.SqlQueryRequest;
import io.ceresdb.models.Result;
import io.ceresdb.util.Utils;

public class QueryLimiterTest {

    @Test(expected = LimitedException.class)
    public void abortQueryLimitTest() throws ExecutionException, InterruptedException {
        final QueryLimiter limiter = new QueryClient.DefaultQueryLimiter(1, new LimitedPolicy.AbortPolicy());
        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("test").sql("select * from test").build();

        // consume the permits
        limiter.acquireAndDo(req, CompletableFuture::new);

        limiter.acquireAndDo(req, this::emptyOk).get();
    }

    @Test
    public void discardWriteLimitTest() throws ExecutionException, InterruptedException {
        final QueryLimiter limiter = new QueryClient.DefaultQueryLimiter(1, new LimitedPolicy.DiscardPolicy());
        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("test").sql("select * from test").build();

        // consume the permits
        limiter.acquireAndDo(req, CompletableFuture::new);

        final Result<SqlQueryOk, Err> ret = limiter.acquireAndDo(req, this::emptyOk).get();

        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(Result.FLOW_CONTROL, ret.getErr().getCode());
        Assert.assertEquals("Query limited by client, acquirePermits=1, maxPermits=1, availablePermits=0.",
                ret.getErr().getError());
    }

    @Test
    public void blockingWriteLimitTest() throws InterruptedException {
        final QueryLimiter limiter = new QueryClient.DefaultQueryLimiter(1, new LimitedPolicy.BlockingPolicy());
        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("test").sql("select * from test").build();

        // consume the permits
        limiter.acquireAndDo(req, CompletableFuture::new);

        final AtomicBoolean alwaysFalse = new AtomicBoolean();
        final Thread t = new Thread(() -> {
            try {
                limiter.acquireAndDo(req, this::emptyOk);
                alwaysFalse.set(true);
            } catch (final Throwable err) {
                // noinspection ConstantConditions
                Assert.assertTrue(err instanceof InterruptedException);
            }
        });
        t.start();

        Assert.assertFalse(alwaysFalse.get());
        Thread.sleep(1000);
        Assert.assertFalse(alwaysFalse.get());
        t.interrupt();
        Assert.assertFalse(alwaysFalse.get());
        Assert.assertTrue(t.isInterrupted());
    }

    @Test
    public void blockingTimeoutWriteLimitTest() throws ExecutionException, InterruptedException {
        final int timeoutSecs = 2;
        final QueryLimiter limiter = new QueryClient.DefaultQueryLimiter(1,
                new LimitedPolicy.BlockingTimeoutPolicy(timeoutSecs, TimeUnit.SECONDS));
        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("test").sql("select * from test").build();

        // consume the permits
        limiter.acquireAndDo(req, CompletableFuture::new);

        final long start = System.nanoTime();
        final Result<SqlQueryOk, Err> ret = limiter.acquireAndDo(req, this::emptyOk).get();
        Assert.assertEquals(timeoutSecs, TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start), 0.3);

        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(Result.FLOW_CONTROL, ret.getErr().getCode());
        Assert.assertEquals("Query limited by client, acquirePermits=1, maxPermits=1, availablePermits=0.",
                ret.getErr().getError());
    }

    @Test(expected = LimitedException.class)
    public void abortOnBlockingTimeoutWriteLimitTest() throws ExecutionException, InterruptedException {
        final int timeoutSecs = 2;
        final QueryLimiter limiter = new QueryClient.DefaultQueryLimiter(1,
                new LimitedPolicy.AbortOnBlockingTimeoutPolicy(timeoutSecs, TimeUnit.SECONDS));
        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("test").sql("select * from test").build();

        // consume the permits
        limiter.acquireAndDo(req, CompletableFuture::new);

        final long start = System.nanoTime();
        try {
            limiter.acquireAndDo(req, this::emptyOk).get();
        } finally {
            Assert.assertEquals(timeoutSecs, TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start), 0.3);
        }
    }

    private CompletableFuture<Result<SqlQueryOk, Err>> emptyOk() {
        return Utils.completedCf(Result.ok(SqlQueryOk.emptyOk()));
    }
}
