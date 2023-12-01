/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;

import org.junit.Assert;
import org.junit.Test;

public class ExecutorServiceHelperTest {

    @Test
    public void shutdownNullTest() {
        Assert.assertTrue(ExecutorServiceHelper.shutdownAndAwaitTermination(null));
    }

    @Test
    public void shutdownNotStart() {
        final ExecutorService e = ThreadPoolUtil.newBuilder().poolName("test_shutdown") //
                .coreThreads(1) //
                .maximumThreads(1) //
                .keepAliveSeconds(100L) //
                .workQueue(new SynchronousQueue<>()) //
                .enableMetric(false) //
                .threadFactory(new NamedThreadFactory("test_shutdown")) //
                .build();
        Assert.assertTrue(ExecutorServiceHelper.shutdownAndAwaitTermination(e));
    }
}
