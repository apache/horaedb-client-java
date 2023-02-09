/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.rpc.interceptors;

import java.util.Map;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import io.ceresdb.common.Tenant;
import io.ceresdb.common.util.AuthUtil;
import io.ceresdb.common.util.Requires;
import io.ceresdb.common.util.Strings;

/**
 * CeresDB auth interceptor.
 *
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
