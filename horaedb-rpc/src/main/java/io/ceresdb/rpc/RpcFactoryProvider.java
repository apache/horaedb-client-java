/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.rpc;

import org.apache.horaedb.common.util.ServiceLoader;

public class RpcFactoryProvider {

    /**
     * SPI RPC factory, default is GrpcFactory
     */
    private static final RpcFactory RPC_FACTORY = ServiceLoader.load(RpcFactory.class) //
            .first();

    /**
     * Get the {@link RpcFactory} impl, base on SPI.
     *
     * @return a shared rpcFactory instance
     */
    public static RpcFactory getRpcFactory() {
        return RPC_FACTORY;
    }
}
