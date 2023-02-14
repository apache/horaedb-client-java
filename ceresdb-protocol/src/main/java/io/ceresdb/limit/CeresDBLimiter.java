/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.limit;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.ceresdb.util.Utils;
import io.ceresdb.common.InFlightLimiter;
import io.ceresdb.common.Limiter;
import io.ceresdb.common.util.MetricsUtil;
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
 */
public abstract class CeresDBLimiter<In, Out> {

    private final Limiter       limiter;
    private final LimitedPolicy policy;
    private final Histogram     acquireAvailablePermits;

    public CeresDBLimiter(int maxInFlight, LimitedPolicy policy, String metricPrefix) {
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
