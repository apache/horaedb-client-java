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

import com.ceresdb.common.SPI;
import com.google.protobuf.Message;

/**
 * CeresDB grpc impl service factory.
 *
 * @author jiachun.fjc
 */
@SPI
public class GrpcFactory implements RpcFactory {

    @SuppressWarnings("unchecked")
    @Override
    public void register(final MethodDescriptor method, //
                         final Class<?> reqCls, //
                         final Object defaultReqIns, //
                         final Object defaultRespIns) {
        getMarshallerRegistry() //
                .registerMarshaller(method, (Class<? extends Message>) reqCls, (Message) defaultReqIns,
                        (Message) defaultRespIns);
    }

    @Override
    public RpcClient createRpcClient(final ConfigHelper<RpcClient> helper) {
        final RpcClient rpcClient = new GrpcClient(getMarshallerRegistry());
        if (helper != null) {
            helper.config(rpcClient);
        }
        return rpcClient;
    }

    protected MarshallerRegistry getMarshallerRegistry() {
        return MarshallerRegistry.DefaultMarshallerRegistry.INSTANCE;
    }
}
