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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceresdb.common.Display;
import com.ceresdb.common.Endpoint;
import com.ceresdb.common.Lifecycle;
import com.ceresdb.common.util.Clock;
import com.ceresdb.common.util.Cpus;
import com.ceresdb.common.util.MetricsUtil;
import com.ceresdb.common.util.Requires;
import com.ceresdb.common.util.SharedScheduledPool;
import com.ceresdb.common.util.Spines;
import com.ceresdb.common.util.TopKSelector;
import com.ceresdb.errors.RouteTableException;
import com.ceresdb.options.RouterOptions;
import com.ceresdb.proto.Storage;
import com.ceresdb.rpc.Context;
import com.ceresdb.rpc.Observer;
import com.ceresdb.rpc.RpcClient;
import com.ceresdb.rpc.errors.RemotingException;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;

/**
 * A route rpc client which cached the routing table information locally
 * and will refresh when the server returns an error code of INVALID_ROUTE
 *
 * @author jiachun.fjc
 */
public class RouterClient implements Lifecycle<RouterOptions>, Display, Iterable<Route> {

    private static final Logger LOG = LoggerFactory.getLogger(RouterClient.class);

    // I don't think they needs to be open to user configuration, so I'll just put a fixed value here
    private static final float CLEAN_CACHE_THRESHOLD   = 0.75f;
    private static final float CLEAN_THRESHOLD         = 0.1f;
    private static final int   MAX_CONTINUOUS_GC_TIMES = 3;

    private static final int  ITEM_COUNT_EACH_REFRESH   = 512;
    private static final long BLOCKING_ROUTE_TIMEOUT_MS = 3000;

    private static final SharedScheduledPool CLEANER_POOL   = Utils.getSharedScheduledPool("route_cache_cleaner", 1);
    private static final SharedScheduledPool REFRESHER_POOL = Utils.getSharedScheduledPool("route_cache_refresher",
            Math.min(4, Cpus.cpus()));

    private ScheduledExecutorService cleaner;
    private ScheduledExecutorService refresher;

    private RouterOptions   opts;
    private RpcClient       rpcClient;
    private RouterByMetrics router;
    private InnerMetrics    metrics;

    private final ConcurrentMap<String, Route> routeCache = new ConcurrentHashMap<>();

    static final class InnerMetrics {
        final Histogram refreshedSize;
        final Histogram cachedSize;
        final Histogram gcTimes;
        final Histogram gcItems;
        final Timer     gcTimer;
        final Timer     refreshTimer;

        private InnerMetrics(final Endpoint name) {
            final String nameSuffix = name.toString();
            this.refreshedSize = MetricsUtil.histogram("route_for_metrics_refreshed_size", nameSuffix);
            this.cachedSize = MetricsUtil.histogram("route_for_metrics_cached_size", nameSuffix);
            this.gcTimes = MetricsUtil.histogram("route_for_metrics_gc_times", nameSuffix);
            this.gcItems = MetricsUtil.histogram("route_for_metrics_gc_items", nameSuffix);
            this.gcTimer = MetricsUtil.timer("route_for_metrics_gc_timer", nameSuffix);
            this.refreshTimer = MetricsUtil.timer("route_for_metrics_refresh_timer", nameSuffix);
        }

        Histogram refreshedSize() {
            return this.refreshedSize;
        }

        Histogram cachedSize() {
            return this.cachedSize;
        }

        Histogram gcTimes() {
            return this.gcTimes;
        }

        Histogram gcItems() {
            return this.gcItems;
        }

        Timer gcTimer() {
            return this.gcTimer;
        }

        Timer refreshTimer() {
            return this.refreshTimer;
        }
    }

    @Override
    public boolean init(final RouterOptions opts) {
        this.opts = Requires.requireNonNull(opts, "RouterClient.opts").copy();
        this.rpcClient = this.opts.getRpcClient();

        final Endpoint address = Requires.requireNonNull(this.opts.getClusterAddress(), "Null.clusterAddress");

        this.router = new RouterByMetrics(address);
        this.metrics = new InnerMetrics(address);

        final long gcPeriod = this.opts.getGcPeriodSeconds();
        if (gcPeriod > 0) {
            this.cleaner = CLEANER_POOL.getObject();
            this.cleaner.scheduleWithFixedDelay(this::gc, Utils.randomInitialDelay(300), gcPeriod, TimeUnit.SECONDS);

            LOG.info("Route table cache cleaner has been started.");
        }

        final long refreshPeriod = this.opts.getRefreshPeriodSeconds();
        if (refreshPeriod > 0) {
            this.refresher = REFRESHER_POOL.getObject();
            this.refresher.scheduleWithFixedDelay(this::refresh, Utils.randomInitialDelay(180), refreshPeriod,
                    TimeUnit.SECONDS);

            LOG.info("Route table cache refresher has been started.");
        }

        return true;
    }

    @Override
    public void shutdownGracefully() {
        if (this.rpcClient != null) {
            this.rpcClient.shutdownGracefully();
        }
        if (this.cleaner != null) {
            CLEANER_POOL.returnObject(this.cleaner);
            this.cleaner = null;
        }
        if (this.refresher != null) {
            REFRESHER_POOL.returnObject(this.refresher);
            this.refresher = null;
        }
        clearRouteCache();
    }

    @Override
    public Iterator<Route> iterator() {
        return this.routeCache.values().iterator();
    }

    public Route clusterRoute() {
        return Route.of(this.opts.getClusterAddress());
    }

    public CompletableFuture<Map<String, Route>> routeFor(final Collection<String> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return Utils.completedCf(Collections.emptyMap());
        }

        final Map<String, Route> local = new HashMap<>();
        final List<String> misses = new ArrayList<>();

        metrics.forEach(metric -> {
            final Route r = this.routeCache.get(metric);
            if (r == null) {
                misses.add(metric);
            } else {
                local.put(metric, r);
            }
        });

        if (misses.isEmpty()) {
            return Utils.completedCf(local);
        }

        return routeRefreshFor(misses) // refresh from remote
                .thenApply(remote -> { // then merge result
                    final Map<String, Route> ret;
                    if (remote.size() > local.size()) {
                        remote.putAll(local);
                        ret = remote;
                    } else {
                        local.putAll(remote);
                        ret = local;
                    }
                    return ret;
                }) //
                .thenApply(hits -> { // update cache hits
                    final long now = Clock.defaultClock().getTick();
                    hits.values().forEach(route -> route.tryWeekSetHit(now));
                    return hits;
                });
    }

    public CompletableFuture<Map<String, Route>> routeRefreshFor(final Collection<String> metrics) {
        final long startCall = Clock.defaultClock().getTick();
        return this.router.routeFor(metrics).whenComplete((remote, err) -> {
            if (err == null) {
                this.routeCache.putAll(remote);
                this.metrics.refreshedSize().update(remote.size());
                this.metrics.cachedSize().update(this.routeCache.size());
                this.metrics.refreshTimer().update(Clock.defaultClock().duration(startCall), TimeUnit.MILLISECONDS);

                LOG.info("Route refreshed: {}, cached_size={}.", metrics, this.routeCache.size());
            } else {
                LOG.warn("Route refresh failed: {}.", metrics, err);
            }
        });
    }

    private void blockingRouteRefreshFor(final Collection<String> metrics) {
        try {
            routeRefreshFor(metrics).get(BLOCKING_ROUTE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Fail to blocking refresh route.", e);
        }
    }

    public void clearRouteCacheBy(final Collection<String> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        metrics.forEach(this.routeCache::remove);
    }

    public int clearRouteCache() {
        final int size = this.routeCache.size();
        this.routeCache.clear();
        return size;
    }

    public void refresh() {
        final Collection<String> cachedKeys = this.routeCache.keySet();

        if (cachedKeys.size() <= ITEM_COUNT_EACH_REFRESH) {
            blockingRouteRefreshFor(cachedKeys);
            return;
        }

        final Collection<String> keysToRefresh = Spines.newBuf(ITEM_COUNT_EACH_REFRESH);
        for (final String metric : cachedKeys) {
            keysToRefresh.add(metric);
            if (keysToRefresh.size() >= ITEM_COUNT_EACH_REFRESH) {
                blockingRouteRefreshFor(keysToRefresh);
                keysToRefresh.clear();
            }
        }

        if (!keysToRefresh.isEmpty()) {
            blockingRouteRefreshFor(keysToRefresh);
        }
    }

    public void gc() {
        this.metrics.gcTimer().time(() -> this.metrics.gcTimes().update(gc0(0)));
    }

    private int gc0(final int times) {
        if (this.routeCache.size() < this.opts.getMaxCachedSize() * CLEAN_CACHE_THRESHOLD) {
            LOG.info("Now that the number of cached entries is {}.", this.routeCache.size());
            return times;
        }

        LOG.warn("Now that the number of cached entries [{}] is about to exceed its limit [{}], we need to clean up.",
                //
                this.routeCache.size(), this.opts.getMaxCachedSize());

        final int itemsToGC = (int) (this.routeCache.size() * CLEAN_THRESHOLD);
        if (itemsToGC <= 0) {
            LOG.warn("No more need to be clean.");
            return times;
        }

        final List<String> topK = TopKSelector.selectTopK( //
                this.routeCache.entrySet(), //
                itemsToGC, //
                (o1, o2) -> -Long.compare(o1.getValue().getLastHit(), o2.getValue().getLastHit()) //
        ) //
                .map(Map.Entry::getKey) //
                .collect(Collectors.toList());

        this.metrics.gcItems().update(topK.size());

        clearRouteCacheBy(topK);

        LOG.warn("Cleaned {} entries from route cache, now entries size {}.", itemsToGC, this.routeCache.size());

        if (this.routeCache.size() > this.opts.getMaxCachedSize() * CLEAN_CACHE_THRESHOLD
            && times < MAX_CONTINUOUS_GC_TIMES) {
            LOG.warn("Now we need to work continuously, this will be the {}th attempt.", times + 1);
            return gc0(times + 1);
        }

        return times;
    }

    public <Req, Resp> CompletableFuture<Resp> invoke(final Endpoint endpoint, //
                                                      final Req request, //
                                                      final Context ctx) {
        return invoke(endpoint, request, ctx, -1 /* use default rpc timeout */);
    }

    public <Req, Resp> CompletableFuture<Resp> invoke(final Endpoint endpoint, //
                                                      final Req request, //
                                                      final Context ctx, //
                                                      final long timeoutMs) {
        final CompletableFuture<Resp> future = new CompletableFuture<>();

        try {
            this.rpcClient.invokeAsync(endpoint, request, ctx, new Observer<Resp>() {

                @Override
                public void onNext(final Resp value) {
                    future.complete(value);
                }

                @Override
                public void onError(final Throwable err) {
                    future.completeExceptionally(err);
                }
            }, timeoutMs);

        } catch (final RemotingException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    public <Req, Resp> void invokeServerStreaming(final Endpoint endpoint, //
                                                  final Req request, //
                                                  final Context ctx, //
                                                  final Observer<Resp> observer) {
        try {
            this.rpcClient.invokeServerStreaming(endpoint, request, ctx, observer);
        } catch (final RemotingException e) {
            observer.onError(e);
        }
    }

    public <Req, Resp> Observer<Req> invokeClientStreaming(final Endpoint endpoint, //
                                                           final Req defaultReqIns, //
                                                           final Context ctx, //
                                                           final Observer<Resp> respObserver) {
        try {
            return this.rpcClient.invokeClientStreaming(endpoint, defaultReqIns, ctx, respObserver);
        } catch (final RemotingException e) {
            respObserver.onError(e);
            return new Observer.RejectedObserver<>(e);
        }
    }

    private Collection<Endpoint> reserveAddresses() {
        return this.routeCache.values().stream().map(Route::getEndpoint).collect(Collectors.toSet());
    }

    private boolean checkConn(final Endpoint endpoint, final boolean create) {
        return this.rpcClient.checkConnection(endpoint, create);
    }

    @Override
    public void display(final Printer out) {
        out.println("--- RouterClient ---") //
                .print("opts=") //
                .println(this.opts) //
                .print("routeCache.size=") //
                .println(this.routeCache.size());

        if (this.rpcClient != null) {
            out.println("");
            this.rpcClient.display(out);
        }
    }

    @Override
    public String toString() {
        return "RouterClient{" + //
               "opts=" + opts + //
               ", rpcClient=" + rpcClient + //
               ", router=" + router + //
               '}';
    }

    private class RouterByMetrics implements Router<Collection<String>, Map<String, Route>> {

        private final Endpoint endpoint;

        private RouterByMetrics(Endpoint endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public CompletableFuture<Map<String, Route>> routeFor(final Collection<String> request) {
            if (request == null || request.isEmpty()) {
                return Utils.completedCf(Collections.emptyMap());
            }

            final Storage.RouteRequest req = Storage.RouteRequest.newBuilder().addAllMetrics(request).build();
            final Context ctx = Context.of("call_priority", "100"); // Mysterious trick!!! ＼（＾▽＾）／
            final CompletableFuture<Storage.RouteResponse> f = invokeRpc(req, ctx);

            return f.thenCompose(resp -> {
                if (Utils.isSuccess(resp.getHeader())) {
                    final Map<String, Route> ret = resp.getRoutesList().stream()
                            .collect(Collectors.toMap(Storage.Route::getMetric, this::toRouteObj));
                    return Utils.completedCf(ret);
                }

                return Utils.errorCf(new RouteTableException("Fail to get route table: " + resp.getHeader()));
            });
        }

        private CompletableFuture<Storage.RouteResponse> invokeRpc(final Storage.RouteRequest req, final Context ctx) {
            if (checkConn(this.endpoint, true)) {
                return invoke(this.endpoint, req, ctx);
            }

            LOG.warn("Fail to connect to the cluster address: {}.", this.endpoint);

            final Collection<Endpoint> reserves = reserveAddresses();
            // RR
            int i = 0;
            for (final Endpoint ep : reserves) {
                LOG.warn("Try to invoke to the {}th server {}.", ++i, ep);
                if (checkConn(ep, false)) {
                    return invoke(ep, req, ctx);
                }
            }

            return Utils.errorCf(new RouteTableException("Fail to connect to: " + this.endpoint));
        }

        private Route toRouteObj(final Storage.Route r) {
            final Storage.Endpoint ep = Requires.requireNonNull(r.getEndpoint(), "CeresDBx.Endpoint");
            return Route.of(r.getMetric(), Endpoint.of(ep.getIp(), ep.getPort()), r.getExt());
        }
    }
}
