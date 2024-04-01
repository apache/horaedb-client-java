/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link java.util.concurrent.ThreadPoolExecutor} that can additionally
 * schedule tasks to run after a given delay with a logger witch can print
 * error message for failed execution.
 *
 */
public class LogScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(LogScheduledThreadPoolExecutor.class);

    private final int    corePoolSize;
    private final String name;

    public LogScheduledThreadPoolExecutor(int corePoolSize, String name) {
        super(corePoolSize);
        this.corePoolSize = corePoolSize;
        this.name = name;
    }

    public LogScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, String name) {
        super(corePoolSize, threadFactory);
        this.corePoolSize = corePoolSize;
        this.name = name;
    }

    public LogScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler, String name) {
        super(corePoolSize, handler);
        this.corePoolSize = corePoolSize;
        this.name = name;
    }

    public LogScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory,
                                          RejectedExecutionHandler handler, String name) {
        super(corePoolSize, threadFactory, handler);
        this.corePoolSize = corePoolSize;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    protected void afterExecute(final Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if (t == null && r instanceof Future<?>) {
            try {
                final Future<?> f = (Future<?>) r;
                if (f.isDone()) {
                    f.get();
                }
            } catch (final CancellationException | ExecutionException ee) {
                t = ee.getCause();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        if (t != null) {
            LOG.error("Uncaught exception in pool: {}, {}.", this.name, super.toString(), t);
        }
    }

    @Override
    protected void terminated() {
        super.terminated();

        LOG.info("ThreadPool is terminated: {}, {}.", this.name, super.toString());
    }

    @Override
    public String toString() {
        return "ScheduledThreadPoolExecutor {" + //
               "corePoolSize=" + corePoolSize + //
               ", name='" + name + '\'' + //
               "} " + super.toString();
    }
}
