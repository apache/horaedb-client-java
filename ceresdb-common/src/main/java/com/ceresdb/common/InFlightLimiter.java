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
package com.ceresdb.common;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.ceresdb.common.util.Clock;
import com.ceresdb.common.util.MetricsUtil;
import com.ceresdb.common.util.internal.ThrowUtil;
import com.codahale.metrics.Timer;

/**
 * In-flight limiter.
 *
 * @author jiachun.fjc
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
