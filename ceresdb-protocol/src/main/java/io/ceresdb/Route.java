/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb;

import java.util.concurrent.atomic.AtomicLong;

import io.ceresdb.common.Endpoint;
import io.ceresdb.common.util.Clock;

/**
 * Route info for table.
 *
 */
public class Route {
    private String           table;
    private Endpoint         endpoint;
    private final AtomicLong lastHit = new AtomicLong(Clock.defaultClock().getTick());

    public static Route invalid(final String table) {
        throw new IllegalStateException("Unexpected, invalid route for table: " + table);
    }

    public static Route of(final Endpoint endpoint) {
        return of(null, endpoint);
    }

    public static Route of(final String table, final Endpoint endpoint) {
        final Route r = new Route();
        r.table = table;
        r.endpoint = endpoint;
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
               ", lastHit=" + lastHit.get() + //
               '}';
    }
}
