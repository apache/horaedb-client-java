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
package com.ceresdb.rpc.interceptors;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import com.ceresdb.rpc.Context;

/**
 * Add RPC context to Grpc headers.
 *
 * @author jiachun.fjc
 */
public class ContextToHeadersInterceptor implements ClientInterceptor {

    private static final ThreadLocal<Context> CURRENT_CTX = new ThreadLocal<>();

    public static void setCurrentCtx(final Context ctx) {
        CURRENT_CTX.set(ctx);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method, //
                                                               final CallOptions callOpts, //
                                                               final Channel next) {
        return new HeaderAttachingClientCall<>(next.newCall(method, callOpts));
    }

    private static final class HeaderAttachingClientCall<ReqT, RespT>
            extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

        // Non private to avoid synthetic class
        HeaderAttachingClientCall(ClientCall<ReqT, RespT> delegate) {
            super(delegate);
        }

        @Override
        public void start(final Listener<RespT> respListener, final Metadata headers) {
            final Context ctx = CURRENT_CTX.get();
            if (ctx != null) {
                ctx.entrySet().forEach(e -> headers.put( //
                        Metadata.Key.of(e.getKey(), Metadata.ASCII_STRING_MARSHALLER), //
                        String.valueOf(e.getValue())) //
                );
            }
            CURRENT_CTX.remove();
            super.start(respListener, headers);
        }
    }
}
