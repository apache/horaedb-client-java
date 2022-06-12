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
package com.ceresdb.rpc;

/**
 * CeresDB's RPC service factory.
 *
 * @author jiachun.fjc
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
