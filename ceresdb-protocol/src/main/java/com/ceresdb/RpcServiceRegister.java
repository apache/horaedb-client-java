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
package com.ceresdb;

import com.ceresdb.common.OptKeys;
import com.ceresdb.common.util.SystemPropertyUtil;
import com.ceresdb.proto.Storage;
import com.ceresdb.rpc.MethodDescriptor;
import com.ceresdb.rpc.RpcFactoryProvider;

/**
 *
 * @author jiachun.fjc
 */
public class RpcServiceRegister {

    private static final double WRITE_LIMIT_PERCENT = writeLimitPercent();

    private static final String STORAGE_METHOD_TEMPLATE = "storage.StorageService/%s";

    public static void registerStorageService() {
        // register protobuf serializer
        RpcFactoryProvider.getRpcFactory().register(
                MethodDescriptor.of(String.format(STORAGE_METHOD_TEMPLATE, "Route"), MethodDescriptor.MethodType.UNARY),
                //
                Storage.RouteRequest.class, //
                Storage.RouteRequest.getDefaultInstance(), //
                Storage.RouteResponse.getDefaultInstance());
        RpcFactoryProvider.getRpcFactory().register(
                MethodDescriptor.of(String.format(STORAGE_METHOD_TEMPLATE, "Write"), MethodDescriptor.MethodType.UNARY,
                        WRITE_LIMIT_PERCENT), //
                Storage.WriteRequest.class, //
                Storage.WriteRequest.getDefaultInstance(), //
                Storage.WriteResponse.getDefaultInstance());
        RpcFactoryProvider.getRpcFactory().register(
                MethodDescriptor.of(String.format(STORAGE_METHOD_TEMPLATE, "StreamWrite"),
                        MethodDescriptor.MethodType.CLIENT_STREAMING), //
                Storage.WriteRequest.class, //
                Storage.WriteRequest.getDefaultInstance(), //
                Storage.WriteResponse.getDefaultInstance());
        RpcFactoryProvider.getRpcFactory().register(
                MethodDescriptor.of(String.format(STORAGE_METHOD_TEMPLATE, "Query"), MethodDescriptor.MethodType.UNARY,
                        1 - WRITE_LIMIT_PERCENT), //
                Storage.QueryRequest.class, //
                Storage.QueryRequest.getDefaultInstance(), //
                Storage.QueryResponse.getDefaultInstance());
        RpcFactoryProvider.getRpcFactory().register(
                MethodDescriptor.of(String.format(STORAGE_METHOD_TEMPLATE, "StreamQuery"),
                        MethodDescriptor.MethodType.SERVER_STREAMING), //
                Storage.QueryRequest.class, //
                Storage.QueryRequest.getDefaultInstance(), //
                Storage.QueryResponse.getDefaultInstance());
    }

    private static double writeLimitPercent() {
        try {
            return Math.min(1.0, Double.parseDouble(SystemPropertyUtil.get(OptKeys.WRITE_LIMIT_PERCENT, "0.7")));
        } catch (final Throwable ignored) {
            return 0.7;
        }
    }
}
