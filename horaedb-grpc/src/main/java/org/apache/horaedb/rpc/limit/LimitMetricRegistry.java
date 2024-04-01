/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc.limit;

import java.util.function.Supplier;

import org.apache.horaedb.common.util.MetricsUtil;
import com.netflix.concurrency.limits.MetricRegistry;

/**
 * For tracking metrics in the limiters.
 *
 */
public class LimitMetricRegistry implements MetricRegistry {

    public static final String RPC_LIMITER = "rpc_limiter";

    @Override
    public SampleListener distribution(final String id, final String... tagKVs) {
        return v -> MetricsUtil.histogram(named(id, tagKVs)).update(v.intValue());
    }

    @Override
    public void gauge(final String id, final Supplier<Number> supplier, final String... tagKVs) {
        MetricsUtil.meter(named(id, tagKVs)).mark(supplier.get().intValue());
    }

    @Override
    public Counter counter(final String id, final String... tagKVs) {
        return () -> MetricsUtil.counter(named(id, tagKVs)).inc();
    }

    private static String named(final String id, final String... tagKVs) {
        return MetricsUtil.namedById(RPC_LIMITER + "_" + id, tagKVs);
    }
}
