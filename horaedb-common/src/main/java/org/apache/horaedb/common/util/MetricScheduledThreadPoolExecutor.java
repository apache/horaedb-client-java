/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A {@link java.util.concurrent.ThreadPoolExecutor} that can additionally
 * schedule tasks to run after a given delay with a timer metric
 * which aggregates timing durations and provides duration statistics.
 *
 */
public class MetricScheduledThreadPoolExecutor extends LogScheduledThreadPoolExecutor {

    public MetricScheduledThreadPoolExecutor(int corePoolSize, String name) {
        super(corePoolSize, name);
    }

    public MetricScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, String name) {
        super(corePoolSize, threadFactory, name);
    }

    public MetricScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler, String name) {
        super(corePoolSize, handler, name);
    }

    public MetricScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory,
                                             RejectedExecutionHandler handler, String name) {
        super(corePoolSize, threadFactory, handler, name);
    }

    @Override
    protected void beforeExecute(final Thread t, final Runnable r) {
        super.beforeExecute(t, r);
        ThreadPoolMetricRegistry.start();
    }

    @Override
    protected void afterExecute(final Runnable r, final Throwable t) {
        ThreadPoolMetricRegistry.metricRegistry() //
                .timer("scheduled_thread_pool." + getName()) //
                .update(ThreadPoolMetricRegistry.finish(), TimeUnit.MILLISECONDS);
        super.afterExecute(r, t);
    }
}
