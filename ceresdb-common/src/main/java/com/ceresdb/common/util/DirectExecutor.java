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
 * An executor that run task directly.
 *
 * @author jiachun.fjc
 */
public class DirectExecutor implements Executor {
    private final String name;
    private final Timer  executeTimer;

    public DirectExecutor(final String name) {
        this.name = name;
        this.executeTimer = MetricsUtil.timer("direct_executor_timer", name);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void execute(final Runnable cmd) {
        this.executeTimer.time(cmd);
    }

    @Override
    public String toString() {
        return "DirectExecutor{" + "name='" + name + '\'' + '}';
    }
}
