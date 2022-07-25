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

import com.codahale.metrics.MetricRegistry;

/**
 * A global timer metric registry for thread pool, use threadLocal to pass timer context.
 *
 * @author jiachun.fjc
 */
public class ThreadPoolMetricRegistry {

    private static final ThreadLocal<Long> TIME_THREAD_LOCAL = ThreadLocal
            .withInitial(() -> Clock.defaultClock().getTick());

    /**
     * Return the global registry of metric instances.
     */
    public static MetricRegistry metricRegistry() {
        return MetricsUtil.metricRegistry();
    }

    public static void start() {
        TIME_THREAD_LOCAL.set(Clock.defaultClock().getTick());
    }

    public static long finish() {
        return Clock.defaultClock().duration(TIME_THREAD_LOCAL.get());
    }
}
