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
package io.ceresdb;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.ceresdb.util.Utils;

/**
 * A route rpc client which implement RouteMode.Proxy
 *
 * cached nothing about table information, and return
 * clusterAddress directly
 */
public class ProxyRouterClient extends RouterClient {

    @Override
    public CompletableFuture<Map<String, Route>> routeFor(final Collection<String> metrics) {
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
