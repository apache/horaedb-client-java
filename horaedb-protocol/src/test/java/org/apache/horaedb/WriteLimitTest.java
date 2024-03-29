/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.WriteClient;
import org.apache.horaedb.WriteClient;
import org.junit.Assert;
import org.junit.Test;

import org.apache.horaedb.errors.LimitedException;
import org.apache.horaedb.limit.LimitedPolicy;
import org.apache.horaedb.limit.WriteLimiter;
import org.apache.horaedb.models.Err;
import org.apache.horaedb.models.Point;
import org.apache.horaedb.models.Result;
import org.apache.horaedb.models.WriteOk;
import org.apache.horaedb.util.TestUtil;
import org.apache.horaedb.util.Utils;

public class WriteLimitTest {

    @Test(expected = LimitedException.class)
    public void abortWriteLimitTest() throws ExecutionException, InterruptedException {
        final WriteLimiter limiter = new WriteClient.DefaultWriteLimiter(1, new LimitedPolicy.AbortPolicy());
        final List<Point> points = TestUtil.newMultiTablePoints("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(points, CompletableFuture::new);

        limiter.acquireAndDo(points, this::emptyOk).get();
    }

    @Test
    public void discardWriteLimitTest() throws ExecutionException, InterruptedException {
        final WriteLimiter limiter = new WriteClient.DefaultWriteLimiter(1, new LimitedPolicy.DiscardPolicy());
        final List<Point> points = TestUtil.newMultiTablePoints("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(points, CompletableFuture::new);

        final Result<WriteOk, Err> ret = limiter.acquireAndDo(points, this::emptyOk).get();

        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(Result.FLOW_CONTROL, ret.getErr().getCode());
        Assert.assertEquals("Write limited by client, acquirePermits=4, maxPermits=1, availablePermits=0.",
                ret.getErr().getError());
    }

    @Test
    public void blockingWriteLimitTest() throws InterruptedException {
        final WriteLimiter limiter = new WriteClient.DefaultWriteLimiter(1, new LimitedPolicy.BlockingPolicy());
        final List<Point> points = TestUtil.newMultiTablePoints("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(points, CompletableFuture::new);

        final AtomicBoolean alwaysFalse = new AtomicBoolean();
        final Thread t = new Thread(() -> {
            try {
                limiter.acquireAndDo(points, this::emptyOk);
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
        final WriteLimiter limiter = new WriteClient.DefaultWriteLimiter(1,
                new LimitedPolicy.BlockingTimeoutPolicy(timeoutSecs, TimeUnit.SECONDS));
        final List<Point> points = TestUtil.newMultiTablePoints("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(points, CompletableFuture::new);

        final long start = System.nanoTime();
        final Result<WriteOk, Err> ret = limiter.acquireAndDo(points, this::emptyOk).get();
        Assert.assertEquals(timeoutSecs, TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start), 0.3);

        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(Result.FLOW_CONTROL, ret.getErr().getCode());
        Assert.assertEquals("Write limited by client, acquirePermits=4, maxPermits=1, availablePermits=0.",
                ret.getErr().getError());
    }

    @Test(expected = LimitedException.class)
    public void abortOnBlockingTimeoutWriteLimitTest() throws ExecutionException, InterruptedException {
        final int timeoutSecs = 2;
        final WriteLimiter limiter = new WriteClient.DefaultWriteLimiter(1,
                new LimitedPolicy.AbortOnBlockingTimeoutPolicy(timeoutSecs, TimeUnit.SECONDS));
        final List<Point> points = TestUtil.newMultiTablePoints("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(points, CompletableFuture::new);

        final long start = System.nanoTime();
        try {
            limiter.acquireAndDo(points, this::emptyOk).get();
        } finally {
            Assert.assertEquals(timeoutSecs, TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start), 0.3);
        }
    }

    private CompletableFuture<Result<WriteOk, Err>> emptyOk() {
        return Utils.completedCf(Result.ok(WriteOk.emptyOk()));
    }
}
