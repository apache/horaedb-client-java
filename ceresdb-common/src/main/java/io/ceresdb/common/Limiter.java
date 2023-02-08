/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
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
