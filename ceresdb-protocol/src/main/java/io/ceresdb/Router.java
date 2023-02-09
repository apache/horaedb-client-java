/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb;

import java.util.concurrent.CompletableFuture;

/**
 * A RPC router for CeresDB.
 *
 */
public interface Router<Req, Resp> {

    /**
     * For a given request return the routing decision for the call.
     *
     * @param request request
     * @return a endpoint for the call
     */
    CompletableFuture<Resp> routeFor(final Req request);
}
