/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

import com.codahale.metrics.MetricRegistry;

/**
 * A global timer metric registry for thread pool, use threadLocal to pass timer context.
 *
 */
public class ThreadPoolMetricRegistry {

    private static final ThreadLocal<Long> TIME_THREAD_LOCAL = ThreadLocal
            .withInitial(() -> Clock.defaultClock().getTick());

    /**
     * Return the global registry of metric instances.
     */
    public static MetricRegistry metricRegistry() {
        return MetricsUtil.metricRegistry();
    }

    public static void start() {
        TIME_THREAD_LOCAL.set(Clock.defaultClock().getTick());
    }

    public static long finish() {
        return Clock.defaultClock().duration(TIME_THREAD_LOCAL.get());
    }
}
