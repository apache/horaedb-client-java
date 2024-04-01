/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Named thread factory.
 *
 */
public class NamedThreadFactory implements ThreadFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NamedThreadFactory.class);

    private static final AtomicInteger FACTORY_ID = new AtomicInteger(0);

    private final AtomicInteger id = new AtomicInteger(0);
    private final String        name;
    private final boolean       daemon;
    private final int           priority;
    private final ThreadGroup   group;

    public NamedThreadFactory(String name) {
        this(name, false, Thread.NORM_PRIORITY);
    }

    public NamedThreadFactory(String name, boolean daemon) {
        this(name, daemon, Thread.NORM_PRIORITY);
    }

    public NamedThreadFactory(String name, int priority) {
        this(name, false, priority);
    }

    @SuppressWarnings("PMD")
    public NamedThreadFactory(String name, boolean daemon, int priority) {
        this.name = FACTORY_ID.getAndIncrement() + "# " + name + " #";
        this.daemon = daemon;
        this.priority = priority;
        final SecurityManager s = System.getSecurityManager();
        this.group = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Thread newThread(final Runnable r) {
        Requires.requireNonNull(r, "runnable");

        final String name2 = this.name + this.id.getAndIncrement();
        final Runnable r2 = wrapRunnable(r);
        final Thread t = wrapThread(this.group, r2, name2);

        try {
            if (t.isDaemon() != this.daemon) {
                t.setDaemon(this.daemon);
            }

            if (t.getPriority() != this.priority) {
                t.setPriority(this.priority);
            }
        } catch (final Exception ignored) {
            // Doesn't matter even if failed to set.
        }

        LOG.info("Creates new {}.", t);

        return t;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }

    protected Runnable wrapRunnable(final Runnable r) {
        return r; // InternalThreadLocalRunnable.wrap(r)
    }

    protected Thread wrapThread(final ThreadGroup group, final Runnable r, final String name) {
        return new Thread(group, r, name);
    }
}
