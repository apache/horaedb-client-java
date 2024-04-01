/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc;

/**
 * HoraeDB's RPC service factory.
 *
 */
public interface RpcFactory {

    /**
     * Register default request instance.
     *
     * @param method         method name and type
     * @param reqCls         request class
     * @param defaultReqIns  default request instance
     * @param defaultRespIns default response instance
     */
    void register(final MethodDescriptor method, //
                  final Class<?> reqCls, //
                  final Object defaultReqIns, //
                  final Object defaultRespIns);

    /**
     * Creates a RPC client.
     *
     * @return a new RPC client instance
     */
    default RpcClient createRpcClient() {
        return createRpcClient(null);
    }

    /**
     * Creates a RPC client.
     *
     * @param helper config helper for RPC client impl
     * @return a new RPC client instance
     */
    RpcClient createRpcClient(final ConfigHelper<RpcClient> helper);

    default ConfigHelper<RpcClient> defaultClientConfigHelper(@SuppressWarnings("unused") final RpcOptions opts) {
        return null;
    }

    interface ConfigHelper<T> {

        void config(final T instance);
    }
}
