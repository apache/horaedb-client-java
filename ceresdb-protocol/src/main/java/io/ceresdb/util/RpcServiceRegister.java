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
// Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
package io.ceresdb.util;

import io.ceresdb.common.OptKeys;
import io.ceresdb.common.util.SystemPropertyUtil;
import io.ceresdb.proto.internal.Storage;
import io.ceresdb.rpc.MethodDescriptor;
import io.ceresdb.rpc.RpcFactoryProvider;

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
                MethodDescriptor.of(String.format(STORAGE_METHOD_TEMPLATE, "SqlQuery"),
                        MethodDescriptor.MethodType.UNARY, 1 - WRITE_LIMIT_PERCENT), //
                Storage.SqlQueryRequest.class, //
                Storage.SqlQueryRequest.getDefaultInstance(), //
                Storage.SqlQueryResponse.getDefaultInstance());
        RpcFactoryProvider.getRpcFactory().register(
                MethodDescriptor.of(String.format(STORAGE_METHOD_TEMPLATE, "StreamSqlQuery"),
                        MethodDescriptor.MethodType.SERVER_STREAMING), //
                Storage.SqlQueryRequest.class, //
                Storage.SqlQueryRequest.getDefaultInstance(), //
                Storage.SqlQueryResponse.getDefaultInstance());
    }

    private static double writeLimitPercent() {
        try {
            return Math.min(1.0, Double.parseDouble(SystemPropertyUtil.get(OptKeys.WRITE_LIMIT_PERCENT, "0.7")));
        } catch (final Throwable ignored) {
            return 0.7;
        }
    }
}
