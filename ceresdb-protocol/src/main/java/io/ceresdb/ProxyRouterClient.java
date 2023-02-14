/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.ceresdb.models.RequestContext;
import io.ceresdb.util.Utils;

/**
 * A route rpc client which implement RouteMode.Proxy
 *
 * cached nothing about table information, and return
 * clusterAddress directly
 */
public class ProxyRouterClient extends RouterClient {

    @Override
    public CompletableFuture<Map<String, Route>> routeFor(final RequestContext reqCtx,
                                                          final Collection<String> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return Utils.completedCf(Collections.emptyMap());
        }

        final Map<String, Route> routeMap = new HashMap<>();

        metrics.forEach(table -> {
            Route route = new Route();
            route.setEndpoint(this.opts.getClusterAddress());
            route.setTable(table);
            routeMap.put(table, route);
        });
        return Utils.completedCf(routeMap);
    }

}
