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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.ceresdb.errors.LimitedException;
import com.ceresdb.models.Err;
import com.ceresdb.models.Result;
import com.ceresdb.models.Rows;
import com.ceresdb.models.WriteOk;
import com.ceresdb.util.TestUtil;

/**
 * @author jiachun.fjc
 */
public class WriteLimitTest {

    @Test(expected = LimitedException.class)
    public void abortWriteLimitTest() throws ExecutionException, InterruptedException {
        final WriteLimiter limiter = new WriteClient.DefaultWriteLimiter(1, new LimitedPolicy.AbortPolicy());
        final Collection<Rows> rows = TestUtil.newListOfRows("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(rows, CompletableFuture::new);

        limiter.acquireAndDo(rows, this::emptyOk).get();
    }

    @Test
    public void discardWriteLimitTest() throws ExecutionException, InterruptedException {
        final WriteLimiter limiter = new WriteClient.DefaultWriteLimiter(1, new LimitedPolicy.DiscardPolicy());
        final Collection<Rows> rows = TestUtil.newListOfRows("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(rows, CompletableFuture::new);

        final Result<WriteOk, Err> ret = limiter.acquireAndDo(rows, this::emptyOk).get();

        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(Result.FLOW_CONTROL, ret.getErr().getCode());
        Assert.assertEquals("Write limited by client, acquirePermits=4, maxPermits=1, availablePermits=0.",
                ret.getErr().getError());
    }

    @Test
    public void blockingWriteLimitTest() throws InterruptedException {
        final WriteLimiter limiter = new WriteClient.DefaultWriteLimiter(1, new LimitedPolicy.BlockingPolicy());
        final Collection<Rows> rows = TestUtil.newListOfRows("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(rows, CompletableFuture::new);

        final AtomicBoolean alwaysFalse = new AtomicBoolean();
        final Thread t = new Thread(() -> {
            try {
                limiter.acquireAndDo(rows, this::emptyOk);
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
        final Collection<Rows> rows = TestUtil.newListOfRows("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(rows, CompletableFuture::new);

        final long start = System.nanoTime();
        final Result<WriteOk, Err> ret = limiter.acquireAndDo(rows, this::emptyOk).get();
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
        final Collection<Rows> rows = TestUtil.newListOfRows("test1", "test2");

        // consume the permits
        limiter.acquireAndDo(rows, CompletableFuture::new);

        final long start = System.nanoTime();
        try {
            limiter.acquireAndDo(rows, this::emptyOk).get();
        } finally {
            Assert.assertEquals(timeoutSecs, TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start), 0.3);
        }
    }

    private CompletableFuture<Result<WriteOk, Err>> emptyOk() {
        return Utils.completedCf(Result.ok(WriteOk.emptyOk()));
    }
}
