/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

import java.util.concurrent.Executor;

import org.apache.horaedb.common.util.DirectExecutor;
import org.apache.horaedb.common.util.RefCell;
import org.junit.Assert;
import org.junit.Test;

public class DirectExecutorTest {

    @Test
    public void selfExecuteTest() {
        final Executor executor = new DirectExecutor("test");
        final Thread t = Thread.currentThread();
        final RefCell<Thread> t2 = new RefCell<>();
        executor.execute(() -> t2.set(Thread.currentThread()));
        Assert.assertSame(t, t2.get());
    }
}
