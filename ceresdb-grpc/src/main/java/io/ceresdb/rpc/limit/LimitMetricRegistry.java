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
package io.ceresdb.rpc.limit;

import java.util.function.Supplier;

import io.ceresdb.common.util.MetricsUtil;
import com.netflix.concurrency.limits.MetricRegistry;

/**
 * For tracking metrics in the limiters.
 *
 * @author jiachun.fjc
 */
public class LimitMetricRegistry implements MetricRegistry {

    public static final String RPC_LIMITER = "rpc_limiter";

    @Override
    public SampleListener distribution(final String id, final String... tagKVs) {
        return v -> MetricsUtil.histogram(named(id, tagKVs)).update(v.intValue());
    }

    @Override
    public void gauge(final String id, final Supplier<Number> supplier, final String... tagKVs) {
        MetricsUtil.meter(named(id, tagKVs)).mark(supplier.get().intValue());
    }

    @Override
    public Counter counter(final String id, final String... tagKVs) {
        return () -> MetricsUtil.counter(named(id, tagKVs)).inc();
    }

    private static String named(final String id, final String... tagKVs) {
        return MetricsUtil.namedById(RPC_LIMITER + "_" + id, tagKVs);
    }
}
