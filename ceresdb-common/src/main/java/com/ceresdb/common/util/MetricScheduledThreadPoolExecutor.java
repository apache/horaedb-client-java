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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A {@link java.util.concurrent.ThreadPoolExecutor} that can additionally
 * schedule tasks to run after a given delay with a timer metric
 * which aggregates timing durations and provides duration statistics.
 *
 * @author jiachun.fjc
 */
public class MetricScheduledThreadPoolExecutor extends LogScheduledThreadPoolExecutor {

    public MetricScheduledThreadPoolExecutor(int corePoolSize, String name) {
        super(corePoolSize, name);
    }

    public MetricScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, String name) {
        super(corePoolSize, threadFactory, name);
    }

    public MetricScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler, String name) {
        super(corePoolSize, handler, name);
    }

    public MetricScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory,
                                             RejectedExecutionHandler handler, String name) {
        super(corePoolSize, threadFactory, handler, name);
    }

    @Override
    protected void beforeExecute(final Thread t, final Runnable r) {
        super.beforeExecute(t, r);
        ThreadPoolMetricRegistry.start();
    }

    @Override
    protected void afterExecute(final Runnable r, final Throwable t) {
        ThreadPoolMetricRegistry.metricRegistry() //
                .timer("scheduled_thread_pool." + getName()) //
                .update(ThreadPoolMetricRegistry.finish(), TimeUnit.MILLISECONDS);
        super.afterExecute(r, t);
    }
}
