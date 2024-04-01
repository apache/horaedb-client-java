/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

import java.util.concurrent.Executor;

import com.codahale.metrics.Timer;

/**
 * A {@link java.util.concurrent.Executor} that with a timer metric
 * which aggregates timing durations and provides duration statistics.
 *
 */
public class MetricExecutor implements Executor {
    private final Executor pool;
    private final String   name;
    private final Timer    executeTimer;

    public MetricExecutor(Executor pool, String name) {
        this.pool = Requires.requireNonNull(pool, "Null.pool");
        this.name = name;
        this.executeTimer = MetricsUtil.timer(name);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void execute(final Runnable cmd) {
        this.pool.execute(() -> this.executeTimer.time(cmd));
    }

    @Override
    public String toString() {
        return "MetricExecutor{" + //
               "pool=" + pool + //
               ", name='" + name + '\'' + //
               '}';
    }
}
