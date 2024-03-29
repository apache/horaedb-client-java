/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc;

import org.apache.horaedb.common.SPI;
import com.google.protobuf.Message;

/**
 * CeresDB grpc impl service factory.
 *
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
