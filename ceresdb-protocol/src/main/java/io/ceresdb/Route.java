/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb;

import java.util.concurrent.atomic.AtomicLong;

import io.ceresdb.common.Endpoint;
import io.ceresdb.common.util.Clock;

/**
 * Route info for metric.
 *
 */
public class Route {
    private String           table;
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
        r.table = metric;
        r.endpoint = endpoint;
        r.ext = ext;
        return r;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
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
               "table='" + table + '\'' + //
               ", endpoint=" + endpoint + //
               ", ext=" + ext + //
               ", lastHit=" + lastHit.get() + //
               '}';
    }
}
