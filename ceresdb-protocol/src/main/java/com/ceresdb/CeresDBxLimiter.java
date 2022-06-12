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
package com.ceresdb;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.ceresdb.common.InFlightLimiter;
import com.ceresdb.common.Limiter;
import com.ceresdb.common.util.MetricsUtil;
import com.codahale.metrics.Histogram;

/**
 * A Write/Query limiter that limits traffic according to the number of
 * query requests or write request's data items in-flight.
 *
 * <p> If the number of permits requested at one time is greater than the
 * total number of permits, we will allow this request under the condition
 * that the available permits are equal to the maximum number of permits,
 * i.e., there are no in-flight requests.
 *
 * @author jiachun.fjc
 */
public abstract class CeresDBxLimiter<In, Out> {

    private final Limiter       limiter;
    private final LimitedPolicy policy;
    private final Histogram     acquireAvailablePermits;

    public CeresDBxLimiter(int maxInFlight, LimitedPolicy policy, String metricPrefix) {
        this.limiter = maxInFlight > 0 ? new InFlightLimiter(maxInFlight, metricPrefix) : null;
        this.policy = policy;
        this.acquireAvailablePermits = MetricsUtil.histogram(metricPrefix, "available_permits");
    }

    public CompletableFuture<Out> acquireAndDo(final In in, final Supplier<CompletableFuture<Out>> action) {
        if (this.limiter == null || this.policy == null) {
            return action.get();
        }

        final int acquirePermits = calculatePermits(in);
        final int maxPermits = this.limiter.maxPermits();
        // If the number of permits requested at one time is greater than the total number of permits,
        // we will allow this request under the condition that the available permits are equal to the
        // maximum number of permits, i.e., there are no in-flight requests.
        final int permits = Math.min(acquirePermits, maxPermits);

        if (permits <= 0) { // fast path
            return action.get();
        }

        this.acquireAvailablePermits.update(this.limiter.availablePermits());

        if (this.policy.acquire(this.limiter, permits)) {
            return action.get().whenComplete((r, e) -> release(permits));
        }
        return Utils.completedCf(rejected(in, acquirePermits, maxPermits));
    }

    public abstract int calculatePermits(final In in);

    public abstract Out rejected(final In in, final RejectedState state);

    private Out rejected(final In in, final int acquirePermits, final int maxPermits) {
        return rejected(in, new RejectedState(acquirePermits, maxPermits, this.limiter.availablePermits()));
    }

    private void release(final int permits) {
        this.limiter.release(permits);
    }

    public static final class RejectedState {
        private final int acquirePermits;
        private final int maxPermits;
        private final int availablePermits;

        public RejectedState(int acquirePermits, int maxPermits, int availablePermits) {
            this.acquirePermits = acquirePermits;
            this.maxPermits = maxPermits;
            this.availablePermits = availablePermits;
        }

        public int acquirePermits() {
            return this.acquirePermits;
        }

        public int maxPermits() {
            return this.maxPermits;
        }

        public int availablePermits() {
            return this.availablePermits;
        }
    }
}
