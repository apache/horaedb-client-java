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
package com.ceresdb.rpc.limit;

import com.ceresdb.common.util.Requires;
import com.netflix.concurrency.limits.MetricIds;
import com.netflix.concurrency.limits.MetricRegistry;
import com.netflix.concurrency.limits.MetricRegistry.SampleListener;
import com.netflix.concurrency.limits.internal.EmptyMetricRegistry;
import com.netflix.concurrency.limits.limit.AbstractLimit;
import com.netflix.concurrency.limits.limit.functions.Log10RootFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Limiter based on TCP Vegas where the limit increases by alpha if the queue_use is small ({@literal <} alpha)
 * and decreases by alpha if the queue_use is large ({@literal >} beta).
 *
 * Queue size is calculated using the formula, 
 *  queue_use = limit − BWE×RTTnoLoad = limit × (1 − RTTnoLoad/RTTactual)
 *
 * For traditional TCP Vegas alpha is typically 2-3 and beta is typically 4-6.  To allow for better growth and 
 * stability at higher limits we set alpha=Max(3, 10% of the current limit) and beta=Max(6, 20% of the current limit)
 *
 * Refer to {@link com.netflix.concurrency.limits.limit.VegasLimit}
 */
public class VegasLimit extends AbstractLimit {

    private static final Logger LOG = LoggerFactory.getLogger(VegasLimit.class);

    private static final Function<Integer, Integer> LOG10 = Log10RootFunction.create(0);

    public static class Builder {
        private int            initialLimit   = 20;
        private int            maxConcurrency = 1000;
        private MetricRegistry registry       = EmptyMetricRegistry.INSTANCE;
        private double         smoothing      = 1.0;

        private Function<Integer, Integer> alphaFunc        = (limit) -> 3 * LOG10.apply(limit);
        private Function<Integer, Integer> betaFunc         = (limit) -> 6 * LOG10.apply(limit);
        private Function<Integer, Integer> thresholdFunc    = LOG10;
        private Function<Double, Double>   increaseFunc     = (limit) -> limit + LOG10.apply(limit.intValue());
        private Function<Double, Double>   decreaseFunc     = (limit) -> limit - LOG10.apply(limit.intValue());
        private int                        probeMultiplier  = 30;
        private boolean                    logOnLimitChange = true;

        private Builder() {
        }

        /**
         * The limiter will probe for a new noload RTT every probeMultiplier * current limit
         * iterations.  Default value is 30.
         *
         * @param probeMultiplier probe multiplier, default is 30
         * @return Chainable builder
         */
        public Builder probeMultiplier(final int probeMultiplier) {
            this.probeMultiplier = probeMultiplier;
            return this;
        }

        public Builder alpha(final int alpha) {
            this.alphaFunc = (ignore) -> alpha;
            return this;
        }

        public Builder threshold(final Function<Integer, Integer> threshold) {
            this.thresholdFunc = threshold;
            return this;
        }

        public Builder alpha(final Function<Integer, Integer> alpha) {
            this.alphaFunc = alpha;
            return this;
        }

        public Builder beta(final int beta) {
            this.betaFunc = (ignore) -> beta;
            return this;
        }

        public Builder beta(final Function<Integer, Integer> beta) {
            this.betaFunc = beta;
            return this;
        }

        public Builder increase(final Function<Double, Double> increase) {
            this.increaseFunc = increase;
            return this;
        }

        public Builder decrease(final Function<Double, Double> decrease) {
            this.decreaseFunc = decrease;
            return this;
        }

        public Builder smoothing(final double smoothing) {
            this.smoothing = smoothing;
            return this;
        }

        public Builder initialLimit(final int initialLimit) {
            this.initialLimit = initialLimit;
            return this;
        }

        public Builder maxConcurrency(final int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        public Builder logOnLimitChange(final boolean logOnLimitChange) {
            this.logOnLimitChange = logOnLimitChange;
            return this;
        }

        public Builder metricRegistry(final MetricRegistry registry) {
            this.registry = registry;
            return this;
        }

        public VegasLimit build() {
            return new VegasLimit(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static VegasLimit newDefault() {
        return newBuilder().build();
    }

    /**
     * Estimated concurrency limit based on our algorithm
     */
    private volatile double estimatedLimit;

    private volatile long rtt_noload = 0;

    /**
     * Maximum allowed limit providing an upper bound failsafe
     */
    private final int maxLimit;

    private final double                     smoothing;
    private final Function<Integer, Integer> alphaFunc;
    private final Function<Integer, Integer> betaFunc;
    private final Function<Integer, Integer> thresholdFunc;
    private final Function<Double, Double>   increaseFunc;
    private final Function<Double, Double>   decreaseFunc;
    private final SampleListener             rttSampleListener;
    private final int                        probeMultiplier;
    private final boolean                    logOnLimitChange;
    private int                              probeCount = 0;
    private double                           probeJitter;

    private VegasLimit(Builder builder) {
        super(builder.initialLimit);
        this.estimatedLimit = builder.initialLimit;
        this.maxLimit = builder.maxConcurrency;
        this.alphaFunc = builder.alphaFunc;
        this.betaFunc = builder.betaFunc;
        this.increaseFunc = builder.increaseFunc;
        this.decreaseFunc = builder.decreaseFunc;
        this.thresholdFunc = builder.thresholdFunc;
        this.smoothing = builder.smoothing;
        this.probeMultiplier = builder.probeMultiplier;
        this.logOnLimitChange = builder.logOnLimitChange;

        resetProbeJitter();

        this.rttSampleListener = builder.registry.distribution(MetricIds.MIN_RTT_NAME);
    }

    private void resetProbeJitter() {
        this.probeJitter = ThreadLocalRandom.current().nextDouble(0.5, 1);
    }

    private boolean shouldProbe() {
        return this.probeJitter * this.probeMultiplier * this.estimatedLimit <= this.probeCount;
    }

    @Override
    protected int _update(final long startTime, final long rtt, final int inflight, final boolean didDrop) {
        Requires.requireTrue(rtt > 0, "rtt must be > 0 but got " + rtt);

        this.probeCount++;
        if (shouldProbe()) {
            LOG.debug("Probe MinRTT {}.", TimeUnit.NANOSECONDS.toMicros(rtt) / 1000.0);
            resetProbeJitter();
            this.probeCount = 0;
            this.rtt_noload = rtt;
            return (int) this.estimatedLimit;
        }

        if (this.rtt_noload == 0 || rtt < this.rtt_noload) {
            LOG.debug("New MinRTT {}.", TimeUnit.NANOSECONDS.toMicros(rtt) / 1000.0);
            this.rtt_noload = rtt;
            return (int) this.estimatedLimit;
        }

        this.rttSampleListener.addSample(getRttMillis(this.rtt_noload));

        return updateEstimatedLimit(rtt, inflight, didDrop);
    }

    private int updateEstimatedLimit(final long rtt, final int inflight, final boolean didDrop) {
        final double currLimit = this.estimatedLimit;

        final int queueSize = (int) Math.ceil(currLimit * (1 - (double) this.rtt_noload / rtt));

        double newLimit;
        // Treat any drop (i.e timeout) as needing to reduce the limit
        if (didDrop) {
            newLimit = this.decreaseFunc.apply(currLimit);
            // Prevent upward drift if not close to the limit
        } else if (inflight * 2 < currLimit) {
            return (int) currLimit;
        } else {
            final int alpha = this.alphaFunc.apply((int) currLimit);
            final int beta = this.betaFunc.apply((int) currLimit);
            final int threshold = this.thresholdFunc.apply((int) currLimit);

            // Aggressive increase when no queuing
            if (queueSize <= threshold) {
                newLimit = currLimit + beta;
                // Increase the limit if queue is still manageable
            } else if (queueSize < alpha) {
                newLimit = this.increaseFunc.apply(currLimit);
                // Detecting latency so decrease
            } else if (queueSize > beta) {
                newLimit = this.decreaseFunc.apply(currLimit);
                // We're within he sweet spot so nothing to do
            } else {
                return (int) currLimit;
            }
        }

        newLimit = Math.max(1, Math.min(this.maxLimit, newLimit));
        newLimit = (1 - this.smoothing) * currLimit + this.smoothing * newLimit;

        if (this.logOnLimitChange && (int) newLimit != (int) currLimit) {
            LOG.info("New limit={}, previous limit={}, minRtt={} ms, winRtt={} ms, queueSize={}.", (int) newLimit,
                    (int) currLimit, TimeUnit.NANOSECONDS.toMicros(this.rtt_noload) / 1000.0,
                    TimeUnit.NANOSECONDS.toMicros(rtt) / 1000.0, queueSize);
        }

        this.estimatedLimit = newLimit;

        return (int) this.estimatedLimit;
    }

    private long getRttMillis(final long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    @Override
    public String toString() {
        return "VegasLimit [limit=" + getLimit() + //
               ", rtt_noload=" + TimeUnit.NANOSECONDS.toMicros(this.rtt_noload) / 1000.0 + //
               " ms]";
    }
}
