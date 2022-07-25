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

import java.util.Map;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import com.ceresdb.common.Tenant;
import com.ceresdb.common.util.AuthUtil;
import com.ceresdb.common.util.Requires;
import com.ceresdb.common.util.Strings;

/**
 * CeresDB auth interceptor.
 *
 * @author jiachun.fjc
 */
public class AuthHeadersInterceptor implements ClientInterceptor {

    private static final ThreadLocal<String> CURRENT_CHILD_TENANT = new ThreadLocal<>();

    private final Tenant tenant;

    public AuthHeadersInterceptor(Tenant tenant) {
        this.tenant = Requires.requireNonNull(tenant, "tenant");
    }

    public static void setCurrentChildTenant(final String childTenant) {
        CURRENT_CHILD_TENANT.set(childTenant);
    }

    public static String getCurrentChildTenant() {
        return CURRENT_CHILD_TENANT.get();
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method, //
                                                               final CallOptions callOpts, //
                                                               final Channel next) {
        return new HeaderAttachingClientCall<>(next.newCall(method, callOpts));
    }

    private final class HeaderAttachingClientCall<ReqT, RespT>
            extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

        // Non private to avoid synthetic class
        HeaderAttachingClientCall(ClientCall<ReqT, RespT> call) {
            super(call);
        }

        @Override
        public void start(final Listener<RespT> respListener, final Metadata headers) {
            final Map<String, String> extraHeaders = AuthUtil.authHeaders(tenant);

            final String childTenant = getCurrentChildTenant();
            if (Strings.isNotBlank(childTenant)) {
                AuthUtil.replaceChildTenant(extraHeaders, childTenant);
            }

            if (!extraHeaders.isEmpty()) {
                extraHeaders.forEach((k, v) -> headers.put(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v));
            }

            super.start(respListener, headers);
        }
    }
}
