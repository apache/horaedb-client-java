/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.horaedb.common.util.Clock;
import org.apache.horaedb.common.util.MetricsUtil;
import org.apache.horaedb.common.util.internal.ThrowUtil;

import com.codahale.metrics.Timer;

/**
 * In-flight limiter.
 *
 */
public class InFlightLimiter implements Limiter {

    private final int       permits;
    private final Semaphore semaphore;
    private final Timer     acquireTimer;

    public InFlightLimiter(int permits, String metricPrefix) {
        this.permits = permits;
        this.semaphore = new Semaphore(permits);
        this.acquireTimer = MetricsUtil.timer(metricPrefix, "wait_time");
    }

    @Override
    public void acquire(final int permits) {
        final long startCall = Clock.defaultClock().getTick();
        try {
            this.semaphore.acquire(permits);
        } catch (final InterruptedException e) {
            ThrowUtil.throwException(e);
        } finally {
            this.acquireTimer.update(Clock.defaultClock().duration(startCall), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean tryAcquire(final int permits, final long timeout, final TimeUnit unit) {
        final long startCall = Clock.defaultClock().getTick();
        try {
            return this.semaphore.tryAcquire(permits, timeout, unit);
        } catch (final InterruptedException e) {
            ThrowUtil.throwException(e);
        } finally {
            this.acquireTimer.update(Clock.defaultClock().duration(startCall), TimeUnit.MILLISECONDS);
        }
        return false;
    }

    @Override
    public void release(final int permits) {
        this.semaphore.release(permits);
    }

    @Override
    public int availablePermits() {
        return this.semaphore.availablePermits();
    }

    @Override
    public int maxPermits() {
        return this.permits;
    }
}
