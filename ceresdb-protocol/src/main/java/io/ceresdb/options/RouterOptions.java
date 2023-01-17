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
package io.ceresdb.options;

import io.ceresdb.RouteMode;
import io.ceresdb.common.Copiable;
import io.ceresdb.common.Endpoint;
import io.ceresdb.rpc.RpcClient;

/**
 * Router options.
 *
 * @author jiachun.fjc
 */
public class RouterOptions implements Copiable<RouterOptions> {

    private RpcClient rpcClient;
    private Endpoint  clusterAddress;
    // Specifies the maximum number of routing table caches. When the number reaches the limit, the ones that
    // have not been used for a long time are cleared first
    private int maxCachedSize = 10_000;
    // The frequency at which the route tables garbage collector is triggered. The default is 60 seconds
    private long gcPeriodSeconds = 60;
    // Refresh frequency of route tables. The background refreshes all route tables periodically. By default,
    // all route tables are refreshed every 30 seconds.
    private long refreshPeriodSeconds = 30;

    private RouteMode routeMode = RouteMode.CLUSTER;

    public RpcClient getRpcClient() {
        return rpcClient;
    }

    public void setRpcClient(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public Endpoint getClusterAddress() {
        return clusterAddress;
    }

    public void setClusterAddress(Endpoint clusterAddress) {
        this.clusterAddress = clusterAddress;
    }

    public int getMaxCachedSize() {
        return maxCachedSize;
    }

    public void setMaxCachedSize(int maxCachedSize) {
        this.maxCachedSize = maxCachedSize;
    }

    public long getGcPeriodSeconds() {
        return gcPeriodSeconds;
    }

    public void setGcPeriodSeconds(long gcPeriodSeconds) {
        this.gcPeriodSeconds = gcPeriodSeconds;
    }

    public long getRefreshPeriodSeconds() {
        return refreshPeriodSeconds;
    }

    public void setRefreshPeriodSeconds(long refreshPeriodSeconds) {
        this.refreshPeriodSeconds = refreshPeriodSeconds;
    }

    public RouteMode getRouteMode() {
        return routeMode;
    }

    public void setRouteMode(RouteMode routeMode) {
        this.routeMode = routeMode;
    }

    @Override
    public RouterOptions copy() {
        final RouterOptions opts = new RouterOptions();
        opts.rpcClient = rpcClient;
        opts.clusterAddress = this.clusterAddress;
        opts.maxCachedSize = this.maxCachedSize;
        opts.gcPeriodSeconds = this.gcPeriodSeconds;
        opts.refreshPeriodSeconds = this.refreshPeriodSeconds;
        opts.routeMode = this.routeMode;
        return opts;
    }

    @Override
    public String toString() {
        return "RouterOptions{" + //
               "rpcClient=" + rpcClient + //
               ", clusterAddress=" + clusterAddress + //
               ", maxCachedSize=" + maxCachedSize + //
               ", gcPeriodSeconds=" + gcPeriodSeconds + //
               ", refreshPeriodSeconds=" + refreshPeriodSeconds + //
               ", routeMode=" + routeMode + //
               '}';
    }

}
