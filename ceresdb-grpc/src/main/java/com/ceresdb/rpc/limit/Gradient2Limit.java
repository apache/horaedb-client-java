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

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.concurrency.limits.MetricIds;
import com.netflix.concurrency.limits.MetricRegistry;
import com.netflix.concurrency.limits.internal.EmptyMetricRegistry;
import com.netflix.concurrency.limits.internal.Preconditions;
import com.netflix.concurrency.limits.limit.AbstractLimit;
import com.netflix.concurrency.limits.limit.measurement.ExpAvgMeasurement;
import com.netflix.concurrency.limits.limit.measurement.Measurement;

/**
 * Concurrency limit algorithm that adjusts the limit based on the gradient of change of the current average RTT and
 * a long term exponentially smoothed average RTT.  Unlike traditional congestion control algorithms we use average
 * instead of minimum since RPC methods can be very bursty due to various factors such as non-homogenous request
 * processing complexity as well as a wide distribution of data size.  We have also found that using minimum can result
 * in an bias towards an impractically low base RTT resulting in excessive load shedding.  An exponential decay is
 * applied to the base RTT so that the value is kept stable yet is allowed to adapt to long term changes in latency
 * characteristics.
 *
 * The core algorithm re-calculates the limit every sampling window (ex. 1 second) using the formula
 *
 *      // Calculate the gradient limiting to the range [0.5, 1.0] to filter outliers
 *      gradient = max(0.5, min(1.0, longtermRtt / currentRtt));
 *
 *      // Calculate the new limit by applying the gradient and allowing for some queuing
 *      newLimit = gradient * currentLimit + queueSize;
 *
 *      // Update the limit using a smoothing factor (default 0.2)
 *      newLimit = currentLimit * (1-smoothing) + newLimit * smoothing
 *
 * The limit can be in one of three main states
 *
 * 1.  Steady state
 *
 * In this state the average RTT is very stable and the current measurement whipsaws around this value, sometimes reducing
 * the limit, sometimes increasing it.
 *
 * 2.  Transition from steady state to load
 *
 * In this state either the RPS to latency has spiked. The gradient is {@literal <} 1.0 due to a growing request queue that
 * cannot be handled by the system. Excessive requests and rejected due to the low limit. The baseline RTT grows using
 * exponential decay but lags the current measurement, which keeps the gradient {@literal <} 1.0 and limit low.
 *
 * 3.  Transition from load to steady state
 *
 * In this state the system goes back to steady state after a prolonged period of excessive load.  Requests aren't rejected
 * and the sample RTT remains low. During this state the long term RTT may take some time to go back to normal and could
 * potentially be several multiples higher than the current RTT.
 *
 * Refer to {@link com.netflix.concurrency.limits.limit.Gradient2Limit}
 */
public class Gradient2Limit extends AbstractLimit {

    private static final Logger LOG = LoggerFactory.getLogger(Gradient2Limit.class);

    public static class Builder {
        private int initialLimit   = 20;
        private int minLimit       = 20;
        private int maxConcurrency = 200;

        private double                     smoothing        = 0.2;
        private Function<Integer, Integer> queueSize        = concurrency -> 4;
        private MetricRegistry             registry         = EmptyMetricRegistry.INSTANCE;
        private int                        longWindow       = 600;
        private double                     rttTolerance     = 1.5;
        private boolean                    logOnLimitChange = true;

        /**
         * Initial limit used by the limiter
         *
         * @param initialLimit initial limit
         * @return Chainable builder
         */
        public Builder initialLimit(final int initialLimit) {
            this.initialLimit = initialLimit;
            return this;
        }

        /**
         * Minimum concurrency limit allowed.  The minimum helps prevent the algorithm
         * from adjust the limit too far down.  Note that this limit is not desirable
         * when use as backpressure for batch apps.
         *
         * @param minLimit minimum limit
         * @return Chainable builder
         */
        public Builder minLimit(final int minLimit) {
            this.minLimit = minLimit;
            return this;
        }

        /**
         * Maximum allowable concurrency.  Any estimated concurrency will be capped
         * at this value
         *
         * @param maxConcurrency maximum concurrency
         * @return Chainable builder
         */
        public Builder maxConcurrency(final int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        /**
         * Fixed amount the estimated limit can grow while latencies remain low
         *
         * @param queueSize fixed queue size
         * @return Chainable builder
         */
        public Builder queueSize(final int queueSize) {
            this.queueSize = (ignore) -> queueSize;
            return this;
        }

        /**
         * Function to dynamically determine the amount the estimated limit can grow while
         * latencies remain low as a function of the current limit.
         *
         * @param queueSize function queue size
         * @return Chainable builder
         */
        public Builder queueSize(final Function<Integer, Integer> queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        /**
         * Tolerance for changes in minimum latency.
         *
         * @param rttTolerance Value {@literal >}= 1.0 indicating how much change in minimum
         *                     latency is acceptable before reducing the limit.  For example,
         *                     a value of 2.0 means that a 2x increase in latency is acceptable.
         * @return Chainable builder
         */
        public Builder rttTolerance(final double rttTolerance) {
            Preconditions.checkArgument(rttTolerance >= 1.0, "Tolerance must be >= 1.0");
            this.rttTolerance = rttTolerance;
            return this;
        }

        /**
         * Smoothing factor to limit how aggressively the estimated limit can shrink
         * when queuing has been detected.
         *
         * @param smoothing Value of 0.0 to 1.0 where 1.0 means the limit is completely
         *                  replicated by the new estimate.
         * @return Chainable builder
         */
        public Builder smoothing(final double smoothing) {
            this.smoothing = smoothing;
            return this;
        }

        /**
         * Registry for reporting metrics about the limiter's internal state.
         *
         * @param registry metric registry
         * @return Chainable builder
         */
        public Builder metricRegistry(final MetricRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder longWindow(final int n) {
            this.longWindow = n;
            return this;
        }

        public Builder logOnLimitChange(final boolean logOnLimitChange) {
            this.logOnLimitChange = logOnLimitChange;
            return this;
        }

        public Gradient2Limit build() {
            return new Gradient2Limit(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Gradient2Limit newDefault() {
        return newBuilder().build();
    }

    /**
     * Estimated concurrency limit based on our algorithm
     */
    private volatile double estimatedLimit;

    /**
     * Tracks a measurement of the short time, and more volatile, RTT meant to represent the
     * current system latency.
     */
    private long lastRtt;

    /**
     * Tracks a measurement of the long term, less volatile, RTT meant to represent the baseline
     * latency.  When the system is under load this number is expect to trend higher.
     */
    private final Measurement longRtt;

    /**
     * Maximum allowed limit providing an upper bound failsafe
     */
    private final int                           maxLimit;
    private final int                           minLimit;
    private final Function<Integer, Integer>    queueSize;
    private final double                        smoothing;
    private final MetricRegistry.SampleListener longRttSampleListener;
    private final MetricRegistry.SampleListener shortRttSampleListener;
    private final MetricRegistry.SampleListener queueSizeSampleListener;
    private final double                        tolerance;
    private final boolean                       logOnLimitChange;

    private Gradient2Limit(Builder builder) {
        super(builder.initialLimit);

        this.estimatedLimit = builder.initialLimit;
        this.maxLimit = builder.maxConcurrency;
        this.minLimit = builder.minLimit;
        this.queueSize = builder.queueSize;
        this.smoothing = builder.smoothing;
        this.tolerance = builder.rttTolerance;
        this.logOnLimitChange = builder.logOnLimitChange;
        this.lastRtt = 0;
        this.longRtt = new ExpAvgMeasurement(builder.longWindow, 10);

        this.longRttSampleListener = builder.registry.distribution(MetricIds.MIN_RTT_NAME);
        this.shortRttSampleListener = builder.registry.distribution(MetricIds.WINDOW_MIN_RTT_NAME);
        this.queueSizeSampleListener = builder.registry.distribution(MetricIds.WINDOW_QUEUE_SIZE_NAME);
    }

    @Override
    public int _update(final long startTime, final long rtt, final int inflight, final boolean didDrop) {
        final double currLimit = this.estimatedLimit;

        final double queueSize = this.queueSize.apply((int) currLimit);

        this.lastRtt = rtt;
        final double shortRtt = (double) rtt;
        final double longRtt = this.longRtt.add(rtt).doubleValue();

        this.shortRttSampleListener.addSample(getRttMillis(shortRtt));
        this.longRttSampleListener.addSample(getRttMillis(longRtt));
        this.queueSizeSampleListener.addSample(queueSize);

        // If the long RTT is substantially larger than the short RTT then reduce the long RTT measurement.
        // This can happen when latency returns to normal after a prolonged prior of excessive load.  Reducing the
        // long RTT without waiting for the exponential smoothing helps bring the system back to steady state.
        if (longRtt / shortRtt > 2) {
            this.longRtt.update(current -> current.doubleValue() * 0.95);
        }

        // Don't grow the limit if we are app limited
        if (inflight < currLimit / 2) {
            return (int) currLimit;
        }

        // Rtt could be higher than rtt_noload because of smoothing rtt noload updates
        // so set to 1.0 to indicate no queuing.  Otherwise calculate the slope and don't
        // allow it to be reduced by more than half to avoid aggressive load-shedding due to 
        // outliers.
        final double gradient = Math.max(0.5, Math.min(1.0, this.tolerance * longRtt / shortRtt));
        double newLimit = currLimit * gradient + queueSize;
        newLimit = currLimit * (1 - this.smoothing) + newLimit * this.smoothing;
        newLimit = Math.max(this.minLimit, Math.min(this.maxLimit, newLimit));

        if (this.logOnLimitChange && (int) currLimit != (int) newLimit) {
            LOG.info("New limit={}, previous limit={}, shortRtt={} ms, longRtt={} ms, queueSize={}, gradient={}.",
                    (int) newLimit, (int) currLimit, getLastRtt(TimeUnit.MICROSECONDS) / 1000.0,
                    getRttNoLoad(TimeUnit.MICROSECONDS) / 1000.0, queueSize, gradient);
        }

        this.estimatedLimit = newLimit;

        return (int) this.estimatedLimit;
    }

    public long getLastRtt(final TimeUnit units) {
        return units.convert(this.lastRtt, TimeUnit.NANOSECONDS);
    }

    public long getRttNoLoad(final TimeUnit units) {
        return units.convert(this.longRtt.get().longValue(), TimeUnit.NANOSECONDS);
    }

    private long getRttMillis(final double nanos) {
        return TimeUnit.NANOSECONDS.toMillis((long) nanos);
    }

    @Override
    public String toString() {
        return "GradientLimit [limit=" + (int) this.estimatedLimit + "]";
    }
}
