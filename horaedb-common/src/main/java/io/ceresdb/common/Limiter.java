/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common;

import java.util.concurrent.TimeUnit;

public interface Limiter {

    /**
     * Acquires the given number of permits from this {@code Limiter},
     * blocking until the request can be granted.
     *
     * @param permits the number of permits to acquire
     */
    void acquire(final int permits);

    /**
     * Acquires the given number of permits from this {@code Limiter}
     * if it can be obtained without exceeding the specified {@code timeout},
     * or returns {@code false} immediately (without waiting) if the permits
     * would not have been granted before the timeout expired.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the permits were acquired, {@code false} otherwise.
     */
    boolean tryAcquire(final int permits, final long timeout, final TimeUnit unit);

    /**
     * @see #tryAcquire(int, long, TimeUnit)
     */
    default boolean tryAcquire(final int permits) {
        return tryAcquire(permits, 0, TimeUnit.NANOSECONDS);
    }

    /**
     * Releases the given number of permits to this {@code Limiter}.
     *
     * @param permits the number of permits to acquire
     */
    void release(final int permits);

    /**
     * Returns the current number of permits available in this {@code Limiter}.
     *
     * @return the number of permits available in this {@code Limiter}
     */
    int availablePermits();

    /**
     * Returns the max number of permits in this {@code Limiter}.
     *
     * @return the max of permits in this {@code Limiter}
     */
    int maxPermits();
}
