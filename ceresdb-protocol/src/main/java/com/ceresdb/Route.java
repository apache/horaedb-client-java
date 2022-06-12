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

import java.util.concurrent.atomic.AtomicLong;

import com.ceresdb.common.Endpoint;
import com.ceresdb.common.util.Clock;

/**
 * Route info for metric.
 *
 * @author jiachun.fjc
 */
public class Route {
    private String           metric;
    private Endpoint         endpoint;
    private Object           ext;
    private final AtomicLong lastHit = new AtomicLong(Clock.defaultClock().getTick());

    public static Route invalid(final String metric) {
        throw new IllegalStateException("Unexpected, invalid route for metric: " + metric);
    }

    public static Route of(final Endpoint endpoint) {
        return of(null, endpoint, null);
    }

    public static Route of(final String metric, final Endpoint endpoint) {
        return of(metric, endpoint, null);
    }

    public static Route of(final String metric, final Endpoint endpoint, final Object ext) {
        final Route r = new Route();
        r.metric = metric;
        r.endpoint = endpoint;
        r.ext = ext;
        return r;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Object getExt() {
        return ext;
    }

    public void setExt(Object ext) {
        this.ext = ext;
    }

    public long getLastHit() {
        return lastHit.get();
    }

    public void tryWeekSetHit(final long lastHit) {
        final long prev = this.lastHit.get();
        if (prev < lastHit) {
            this.lastHit.weakCompareAndSet(prev, lastHit);
        }
    }

    @Override
    public String toString() {
        return "Route{" + //
               "metric='" + metric + '\'' + //
               ", endpoint=" + endpoint + //
               ", ext=" + ext + //
               ", lastHit=" + lastHit.get() + //
               '}';
    }
}
