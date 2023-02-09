/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.options;

import java.util.concurrent.Executor;

import io.ceresdb.limit.LimitedPolicy;
import io.ceresdb.RouteMode;
import io.ceresdb.common.Copiable;
import io.ceresdb.common.Endpoint;
import io.ceresdb.common.Tenant;
import io.ceresdb.common.util.Requires;
import io.ceresdb.rpc.RpcOptions;

/**
 * CeresDB client options.
 *
 */
public class CeresDBOptions implements Copiable<CeresDBOptions> {
    private Endpoint      clusterAddress;
    private Executor      asyncWritePool;
    private Executor      asyncReadPool;
    private Tenant        tenant;
    private RpcOptions    rpcOptions;
    private RouterOptions routerOptions;
    private WriteOptions  writeOptions;
    private QueryOptions  queryOptions;

    public Endpoint getClusterAddress() {
        return clusterAddress;
    }

    public void setClusterAddress(Endpoint clusterAddress) {
        this.clusterAddress = clusterAddress;
    }

    public Executor getAsyncWritePool() {
        return asyncWritePool;
    }

    public void setAsyncWritePool(Executor asyncWritePool) {
        this.asyncWritePool = asyncWritePool;
    }

    public Executor getAsyncReadPool() {
        return asyncReadPool;
    }

    public void setAsyncReadPool(Executor asyncReadPool) {
        this.asyncReadPool = asyncReadPool;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public RpcOptions getRpcOptions() {
        return rpcOptions;
    }

    public void setRpcOptions(RpcOptions rpcOptions) {
        this.rpcOptions = rpcOptions;
    }

    public RouterOptions getRouterOptions() {
        return routerOptions;
    }

    public void setRouterOptions(RouterOptions routerOptions) {
        this.routerOptions = routerOptions;
    }

    public WriteOptions getWriteOptions() {
        return writeOptions;
    }

    public void setWriteOptions(WriteOptions writeOptions) {
        this.writeOptions = writeOptions;
    }

    public QueryOptions getQueryOptions() {
        return queryOptions;
    }

    public void setQueryOptions(QueryOptions queryOptions) {
        this.queryOptions = queryOptions;
    }

    @Override
    public CeresDBOptions copy() {
        final CeresDBOptions copy = new CeresDBOptions();
        copy.clusterAddress = this.clusterAddress;
        copy.asyncWritePool = this.asyncWritePool;
        copy.asyncReadPool = this.asyncReadPool;
        if (this.tenant != null) {
            copy.tenant = this.tenant.copy();
        }
        if (this.rpcOptions != null) {
            copy.rpcOptions = this.rpcOptions.copy();
        }
        if (this.routerOptions != null) {
            copy.routerOptions = this.routerOptions.copy();
        }
        if (this.writeOptions != null) {
            copy.writeOptions = this.writeOptions.copy();
        }
        if (this.queryOptions != null) {
            copy.queryOptions = this.queryOptions.copy();
        }
        return copy;
    }

    @Override
    public String toString() {
        return "CeresDBOptions{" + //
               "clusterAddress=" + clusterAddress + //
               ", asyncWritePool=" + asyncWritePool + //
               ", asyncReadPool=" + asyncReadPool + //
               ", tenant=" + tenant + //
               ", rpcOptions=" + rpcOptions + //
               ", routerOptions=" + routerOptions + //
               ", writeOptions=" + writeOptions + //
               ", queryOptions=" + queryOptions + //
               '}';
    }

    public static CeresDBOptions check(final CeresDBOptions opts) {
        Requires.requireNonNull(opts, "CeresDBOptions.opts");
        Requires.requireNonNull(opts.getClusterAddress(), "CeresDBOptions.clusterAddress");
        Requires.requireNonNull(opts.getTenant(), "CeresDBOptions.tenant");
        Requires.requireNonNull(opts.getRpcOptions(), "CeresDBOptions.rpcOptions");
        Requires.requireNonNull(opts.getRouterOptions(), "CeresDBOptions.RouterOptions");
        Requires.requireNonNull(opts.getRouterOptions().getRouteMode(), "CeresDBOptions.RouterOptions.RouteMode");
        Requires.requireNonNull(opts.getWriteOptions(), "CeresDBOptions.writeOptions");
        Requires.requireNonNull(opts.getQueryOptions(), "CeresDBOptions.queryOptions");
        return opts;
    }

    /**
     * Create a new builder for CeresDBOptions.
     *
     * @param clusterAddress cluster address, for read/write data
     * @Param routeMode direct or proxy in RouteMode
     * @return builder
     */
    public static Builder newBuilder(final Endpoint clusterAddress, RouteMode routeMode) {
        return new Builder(clusterAddress, routeMode);
    }

    /**
     * Create a new builder for CeresDBOptions.
     *
     * @param clusterHost cluster ip/host, for read/write data
     * @param clusterPort cluster port
     * @param routeMode direct or proxy in RouteMode
     * @return builder
     */
    public static Builder newBuilder(final String clusterHost, final int clusterPort, RouteMode routeMode) {
        return newBuilder(Endpoint.of(clusterHost, clusterPort), routeMode);
    }

    public static final class Builder {
        // The only constant address of this cluster.
        private final Endpoint clusterAddress;
        // The routeMode for sdk, only Proxy and Direct support now.
        private RouteMode routeMode;
        // Asynchronous thread pool, which is used to handle various asynchronous tasks in the SDK.
        private Executor asyncWritePool;
        private Executor asyncReadPool;
        // Tenant
        private Tenant tenant;
        // Rpc options, in general, the default configuration is fine.
        private RpcOptions rpcOptions = RpcOptions.newDefault();
        // Write options
        // Maximum data entry per write
        private int maxWriteSize = 512;
        // In the case of routing table failure or some other retry able error, a retry of the write is attempted.
        private int writeMaxRetries = 1;
        // Write flow control: maximum number of data rows in-flight.
        private int maxInFlightWriteRows = 8192;
        // Write flow control: limited policy
        private LimitedPolicy writeLimitedPolicy = LimitedPolicy.defaultWriteLimitedPolicy();
        // Query options
        // In the case of routing table failure, a retry of the read is attempted.
        private int readMaxRetries = 1;
        // Query flow control: maximum number of query requests in-flight.
        private int maxInFlightQueryRequests = 8;
        // Query flow control: limited policy
        private LimitedPolicy queryLimitedPolicy = LimitedPolicy.defaultQueryLimitedPolicy();
        // Specifies the maximum number of routing table caches. When the number reaches the limit, the ones that
        // have not been used for a long time are cleared first
        private int routeTableMaxCachedSize = 10_000;
        // The frequency at which the route tables garbage collector is triggered. The default is 60 seconds.
        private long routeTableGcPeriodSeconds = 60;
        // Refresh frequency of route tables. The background refreshes all route tables periodically. By default,
        // all route tables are refreshed every 30 seconds.
        private long routeTableRefreshPeriodSeconds = 30;

        public Builder(Endpoint clusterAddress, RouteMode routeMode) {
            this.clusterAddress = clusterAddress;
            this.routeMode = routeMode;
        }

        /**
         * Asynchronous thread pool, which is used to handle various asynchronous
         * tasks in the SDK (You are using a purely asynchronous SDK). If you do not
         * set it, there will be a default implementation, which you can reconfigure
         * if the default implementation is not satisfied.
         *
         * Note: We do not close it to free resources, as we view it as shared.
         *
         * @param asyncWritePool async thread pool for write
         * @param asyncReadPool  async thread pool for read
         * @return this builder
         */
        public Builder asyncPool(final Executor asyncWritePool, final Executor asyncReadPool) {
            this.asyncWritePool = asyncWritePool;
            this.asyncReadPool = asyncReadPool;
            return this;
        }

        /**
         * @see #tenant(String, String, String)
         *
         * @param tenant the tenant name
         * @param token  don't tell the secret to anyone, heaven knows
         *               and earth knows, you know and I know.  ＼（＾▽＾）／
         * @return this builder
         */
        public Builder tenant(final String tenant, final String token) {
            this.tenant = Tenant.of(tenant, null, token);
            return this;
        }

        /**
         * Sets a tenant.
         *
         * @param tenant      the tenant name
         * @param childTenant default subtenant, which is used if you do not
         *                    re-specify a subtenant each time you make a call.
         * @param token       don't tell the secret to anyone, heaven knows
         *                    and earth knows, you know and I know.  ＼（＾▽＾）／
         * @return this builder
         */
        public Builder tenant(final String tenant, final String childTenant, final String token) {
            this.tenant = Tenant.of(tenant, childTenant, token);
            return this;
        }

        /**
         * Sets the RPC options, in general, the default configuration is fine.
         *
         * @param rpcOptions the rpc options
         * @return this builder
         */
        public Builder rpcOptions(final RpcOptions rpcOptions) {
            this.rpcOptions = rpcOptions;
            return this;
        }

        /**
         * Maximum data entries per write.
         *
         * @param maxWriteSize maximum data entries
         * @return this builder
         */
        public Builder maxWriteSize(final int maxWriteSize) {
            this.maxWriteSize = maxWriteSize;
            return this;
        }

        /**
         * In the case of routing table failure or some other retry able error,
         * a retry of the write is attempted.
         *
         * @param maxRetries max retries times
         * @return this builder
         */
        public Builder writeMaxRetries(final int maxRetries) {
            this.writeMaxRetries = maxRetries;
            return this;
        }

        /**
         * Write flow control: maximum number of data rows in-flight.
         *
         * @param maxInFlightWriteRows maximum number of data rows in-flight
         * @return this builder
         */
        public Builder maxInFlightWriteRows(final int maxInFlightWriteRows) {
            this.maxInFlightWriteRows = maxInFlightWriteRows;
            return this;
        }

        /**
         * Write flow control: limited policy.
         *
         * @param writeLimitedPolicy the limited policy
         * @return this builder
         */
        public Builder writeLimitedPolicy(final LimitedPolicy writeLimitedPolicy) {
            this.writeLimitedPolicy = writeLimitedPolicy;
            return this;
        }

        /**
         * In the case of routing table failure, a retry of the rpc is attempted.
         *
         * @param maxRetries max retries times
         * @return this builder
         */
        public Builder readMaxRetries(final int maxRetries) {
            this.readMaxRetries = maxRetries;
            return this;
        }

        /**
         * Query flow control: maximum number of query request in-flight.
         *
         * @param maxInFlightQueryRequests maximum number of query requests in-flight
         * @return this builder
         */
        public Builder maxInFlightQueryRequests(final int maxInFlightQueryRequests) {
            this.maxInFlightQueryRequests = maxInFlightQueryRequests;
            return this;
        }

        /**
         * Query flow control: limited policy.
         *
         * @param queryLimitedPolicy the limited policy
         * @return this builder
         */
        public Builder queryLimitedPolicy(final LimitedPolicy queryLimitedPolicy) {
            this.queryLimitedPolicy = queryLimitedPolicy;
            return this;
        }

        /**
         * Specifies the maximum number of routing table caches. When the number reaches
         * the limit, the ones that have not been used for a long time are cleared first.
         *
         * @param routeTableMaxCachedSize max cached size
         * @return this builder
         */
        public Builder routeTableMaxCachedSize(final int routeTableMaxCachedSize) {
            this.routeTableMaxCachedSize = routeTableMaxCachedSize;
            return this;
        }

        /**
         * The frequency at which the route tables garbage collector is triggered. The
         * default is 60 seconds.
         *
         * @param routeTableGcPeriodSeconds gc period for route tables
         * @return this builder
         */
        public Builder routeTableGcPeriodSeconds(final long routeTableGcPeriodSeconds) {
            this.routeTableGcPeriodSeconds = routeTableGcPeriodSeconds;
            return this;
        }

        /**
         * Refresh frequency of route tables. The background refreshes all route tables
         * periodically. By default, all route tables are refreshed every 30 seconds.
         *
         * @param routeTableRefreshPeriodSeconds refresh period for route tables cache
         * @return this builder
         */
        public Builder routeTableRefreshPeriodSeconds(final long routeTableRefreshPeriodSeconds) {
            this.routeTableRefreshPeriodSeconds = routeTableRefreshPeriodSeconds;
            return this;
        }

        /**
         * A good start, happy coding.
         *
         * @return nice things
         */
        public CeresDBOptions build() {
            final CeresDBOptions opts = new CeresDBOptions();
            opts.clusterAddress = this.clusterAddress;
            opts.asyncWritePool = asyncWritePool;
            opts.asyncReadPool = asyncReadPool;
            opts.tenant = this.tenant;
            opts.rpcOptions = this.rpcOptions;
            opts.routerOptions = new RouterOptions();
            opts.routerOptions.setClusterAddress(this.clusterAddress);
            opts.routerOptions.setMaxCachedSize(this.routeTableMaxCachedSize);
            opts.routerOptions.setGcPeriodSeconds(this.routeTableGcPeriodSeconds);
            opts.routerOptions.setRefreshPeriodSeconds(this.routeTableRefreshPeriodSeconds);
            opts.routerOptions.setRouteMode(this.routeMode);

            opts.writeOptions = new WriteOptions();
            opts.writeOptions.setMaxWriteSize(this.maxWriteSize);
            opts.writeOptions.setMaxRetries(this.writeMaxRetries);
            opts.writeOptions.setMaxInFlightWritePoints(this.maxInFlightWriteRows);
            opts.writeOptions.setLimitedPolicy(this.writeLimitedPolicy);
            opts.queryOptions = new QueryOptions();
            opts.queryOptions.setMaxRetries(this.readMaxRetries);
            opts.queryOptions.setMaxInFlightQueryRequests(this.maxInFlightQueryRequests);
            opts.queryOptions.setLimitedPolicy(this.queryLimitedPolicy);
            return CeresDBOptions.check(opts);
        }
    }
}
