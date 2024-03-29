/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

import java.util.concurrent.Executor;

import com.codahale.metrics.Timer;

/**
 * An executor that run task directly.
 *
 */
public class DirectExecutor implements Executor {
    private final String name;
    private final Timer  executeTimer;

    public DirectExecutor(final String name) {
        this.name = name;
        this.executeTimer = MetricsUtil.timer("direct_executor_timer", name);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void execute(final Runnable cmd) {
        this.executeTimer.time(cmd);
    }

    @Override
    public String toString() {
        return "DirectExecutor{" + "name='" + name + '\'' + '}';
    }
}
