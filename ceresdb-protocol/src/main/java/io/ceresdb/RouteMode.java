/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb;

public enum RouteMode {

    /**
     * In this mode, the client does not cache routing information and each request is proxied through the server to the correct server
     */
    PROXY,

    /**
     * In this mode, the client cache routing information. Client find the correct server firstly, and then request to the correct server directly.
     */
    DIRECT
}