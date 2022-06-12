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

import java.util.concurrent.Executor;

import com.codahale.metrics.Timer;

/**
 * A {@link java.util.concurrent.Executor} that with a timer metric
 * which aggregates timing durations and provides duration statistics.
 *
 * @author jiachun.fjc
 */
public class MetricExecutor implements Executor {
    private final Executor pool;
    private final String   name;
    private final Timer    executeTimer;

    public MetricExecutor(Executor pool, String name) {
        this.pool = Requires.requireNonNull(pool, "Null.pool");
        this.name = name;
        this.executeTimer = MetricsUtil.timer(name);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void execute(final Runnable cmd) {
        this.pool.execute(() -> this.executeTimer.time(cmd));
    }

    @Override
    public String toString() {
        return "MetricExecutor{" + //
               "pool=" + pool + //
               ", name='" + name + '\'' + //
               '}';
    }
}
