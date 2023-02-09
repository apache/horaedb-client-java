/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.options;

import java.util.concurrent.Executor;

import io.ceresdb.limit.LimitedPolicy;
import io.ceresdb.RouterClient;
import io.ceresdb.common.Copiable;

/**
 * Query options.
 *
 */
public class QueryOptions implements Copiable<QueryOptions> {

    private RouterClient routerClient;
    private Executor     asyncPool;

    // In the case of routing table failure, a retry of the read is attempted.
    private int maxRetries = 1;
    // Query flow limit: maximum number of query requests in-flight.
    private int           maxInFlightQueryRequests = 8;
    private LimitedPolicy limitedPolicy            = LimitedPolicy.defaultQueryLimitedPolicy();

    public RouterClient getRouterClient() {
        return routerClient;
    }

    public void setRouterClient(RouterClient routerClient) {
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

    public int getMaxInFlightQueryRequests() {
        return maxInFlightQueryRequests;
    }

    public void setMaxInFlightQueryRequests(int maxInFlightQueryRequests) {
        this.maxInFlightQueryRequests = maxInFlightQueryRequests;
    }

    public LimitedPolicy getLimitedPolicy() {
        return limitedPolicy;
    }

    public void setLimitedPolicy(LimitedPolicy limitedPolicy) {
        this.limitedPolicy = limitedPolicy;
    }

    @Override
    public QueryOptions copy() {
        final QueryOptions opts = new QueryOptions();
        opts.routerClient = this.routerClient;
        opts.asyncPool = this.asyncPool;
        opts.maxRetries = this.maxRetries;
        opts.maxInFlightQueryRequests = this.maxInFlightQueryRequests;
        opts.limitedPolicy = this.limitedPolicy;
        return opts;
    }

    @Override
    public String toString() {
        return "QueryOptions{" + //
               "routerClient=" + routerClient + //
               "asyncPool=" + asyncPool + //
               "maxRetries=" + maxRetries + //
               "maxInFlightQueryRequests=" + maxInFlightQueryRequests + //
               "limitedPolicy=" + limitedPolicy + //
               '}';
    }
}
