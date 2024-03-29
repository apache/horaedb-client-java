/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache;

import java.util.concurrent.CompletableFuture;

import org.apache.horaedb.models.RequestContext;

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
    CompletableFuture<Resp> routeFor(final RequestContext reqCtx, final Req request);
}
