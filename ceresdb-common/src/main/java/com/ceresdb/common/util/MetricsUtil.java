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
package com.ceresdb.common.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;

/**
 * In CeresDB client, metrics are required. As for whether to output (log) metrics
 * results, you decide.
 *
 * @author jiachun.fjc
 */
public final class MetricsUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsUtil.class);

    private static final MetricRegistry    METRIC_REGISTRY = new MetricRegistry();
    private static final ScheduledReporter SCHEDULED_REPORTER;

    static {
        final ScheduledExecutorService scheduledPool = ThreadPoolUtil.newScheduledBuilder() //
                .enableMetric(true) //
                .coreThreads(1) //
                .poolName("metrics.reporter") //
                .threadFactory(new NamedThreadFactory("metrics.reporter", true)) //
                .build();
        SCHEDULED_REPORTER = createReporter(scheduledPool);
    }

    private static ScheduledReporter createReporter(final ScheduledExecutorService scheduledPool) {
        try {
            return Slf4jReporter.forRegistry(MetricsUtil.METRIC_REGISTRY) //
                    .withLoggingLevel(Slf4jReporter.LoggingLevel.INFO) //
                    .outputTo(LOG) //
                    .scheduleOn(scheduledPool) //
                    .shutdownExecutorOnStop(true) //
                    .build();
        } catch (final Throwable ex) {
            LOG.warn("Fail to create metrics reporter.", ex);
            return null;
        }
    }

    public static void startScheduledReporter(final long period, final TimeUnit unit) {
        if (SCHEDULED_REPORTER != null) {
            LOG.info("Starting the metrics scheduled reporter.");
            SCHEDULED_REPORTER.start(period, unit);
        }
    }

    public static void stopScheduledReporterAndDestroy() {
        if (SCHEDULED_REPORTER != null) {
            LOG.info("Stopping the metrics scheduled reporter.");
            SCHEDULED_REPORTER.stop();
        }
    }

    public static void reportImmediately() {
        SCHEDULED_REPORTER.report();
    }

    /**
     * Return the global registry of metric instances.
     */
    public static MetricRegistry metricRegistry() {
        return METRIC_REGISTRY;
    }

    /**
     * Return the {@link Meter} registered under this name; or create
     * and register a new {@link Meter} if none is registered.
     */
    public static Meter meter(final Object name) {
        return METRIC_REGISTRY.meter(named(name));
    }

    /**
     * Return the {@link Meter} registered under this name; or create
     * and register a new {@link Meter} if none is registered.
     */
    public static Meter meter(final Object... names) {
        return METRIC_REGISTRY.meter(named(names));
    }

    /**
     * Return the {@link Timer} registered under this name; or create
     * and register a new {@link Timer} if none is registered.
     */
    public static Timer timer(final Object name) {
        return METRIC_REGISTRY.timer(named(name));
    }

    /**
     * Return the {@link Timer} registered under this name; or create
     * and register a new {@link Timer} if none is registered.
     */
    public static Timer timer(final Object... names) {
        return METRIC_REGISTRY.timer(named(names));
    }

    /**
     * Return the {@link Counter} registered under this name; or create
     * and register a new {@link Counter} if none is registered.
     */
    public static Counter counter(final Object name) {
        return METRIC_REGISTRY.counter(named(name));
    }

    /**
     * Return the {@link Counter} registered under this name; or create
     * and register a new {@link Counter} if none is registered.
     */
    public static Counter counter(final Object... names) {
        return METRIC_REGISTRY.counter(named(names));
    }

    /**
     * Return the {@link Histogram} registered under this name; or create
     * and register a new {@link Histogram} if none is registered.
     */
    public static Histogram histogram(final Object name) {
        return METRIC_REGISTRY.histogram(named(name));
    }

    /**
     * Return the {@link Histogram} registered under this name; or create
     * and register a new {@link Histogram} if none is registered.
     */
    public static Histogram histogram(final Object... names) {
        return METRIC_REGISTRY.histogram(named(names));
    }

    public static String named(final Object name) {
        return String.valueOf(name);
    }

    public static String namedById(final String id, final String... names) {
        final StringBuilder buf = StringBuilderHelper.get();
        buf.append(id);
        named0(buf, names);
        return buf.toString();
    }

    public static String named(final Object... names) {
        final StringBuilder buf = StringBuilderHelper.get();
        named0(buf, names);
        return buf.toString();
    }

    private static void named0(final StringBuilder buf, final Object... names) {
        for (final Object name : names) {
            if (buf.length() > 0) {
                buf.append('_');
            }
            buf.append(name);
        }
    }

    private static void named0(final StringBuilder buf, final String... names) {
        for (final String name : names) {
            if (buf.length() > 0) {
                buf.append('_');
            }
            buf.append(name);
        }
    }

    private MetricsUtil() {
    }
}
