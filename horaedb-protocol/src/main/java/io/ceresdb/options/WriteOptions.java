/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.options;

import java.util.concurrent.Executor;

import io.ceresdb.limit.LimitedPolicy;
import io.ceresdb.RouterClient;
import org.apache.horaedb.common.Copiable;

/**
 * Write options.
 *
 */
public class WriteOptions implements Copiable<WriteOptions> {

    private String       database;
    private RouterClient routerClient;
    private Executor     asyncPool;

    // Maximum data entry per write
    private int maxRetries = 1;
    // In the case of routing table failure or some other retry able error, a retry of the write is attempted.
    private int maxWriteSize = 512;
    // Write flow limit: maximum number of data points in-flight.
    private int           maxInFlightWritePoints = 8192;
    private LimitedPolicy limitedPolicy          = LimitedPolicy.defaultWriteLimitedPolicy();

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public RouterClient getRoutedClient() {
        return routerClient;
    }

    public void setRoutedClient(RouterClient routerClient) {
        this.routerClient = routerClient;
    }

    public Executor getAsyncPool() {
        return asyncPool;
    }

    public void setAsyncPool(Executor asyncPool) {
        this.asyncPool = asyncPool;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxWriteSize() {
        return maxWriteSize;
    }

    public void setMaxWriteSize(int maxWriteSize) {
        this.maxWriteSize = maxWriteSize;
    }

    public int getMaxInFlightWritePoints() {
        return maxInFlightWritePoints;
    }

    public void setMaxInFlightWritePoints(int maxInFlightWritePoints) {
        this.maxInFlightWritePoints = maxInFlightWritePoints;
    }

    public LimitedPolicy getLimitedPolicy() {
        return limitedPolicy;
    }

    public void setLimitedPolicy(LimitedPolicy limitedPolicy) {
        this.limitedPolicy = limitedPolicy;
    }

    @Override
    public WriteOptions copy() {
        final WriteOptions opts = new WriteOptions();
        opts.database = this.database;
        opts.routerClient = this.routerClient;
        opts.asyncPool = this.asyncPool;
        opts.maxRetries = this.maxRetries;
        opts.maxWriteSize = this.maxWriteSize;
        opts.maxInFlightWritePoints = this.maxInFlightWritePoints;
        opts.limitedPolicy = this.limitedPolicy;
        return opts;
    }

    @Override
    public String toString() {
        return "WriteOptions{" + //
               ", database=" + database + //
               ", routerClient=" + routerClient + //
               ", globalAsyncPool=" + asyncPool + //
               ", maxRetries=" + maxRetries + //
               ", maxWriteSize=" + maxWriteSize + //
               ", maxInFlightWritePoints=" + maxInFlightWritePoints + //
               ", limitedPolicy=" + limitedPolicy + //
               '}';
    }
}
