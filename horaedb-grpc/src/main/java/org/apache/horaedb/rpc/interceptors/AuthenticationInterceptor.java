/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc.interceptors;

import io.grpc.*;

import java.util.Base64;

public class AuthenticationInterceptor implements ClientInterceptor {
    private final String token;

    public AuthenticationInterceptor(String user, String password) {
        // Build token
        this.token = Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method,
                                                               final CallOptions callOpts, Channel next) {

        return new AuthenticationAttachingClientCall<>(next.newCall(method, callOpts), token);
    }

    private static final class AuthenticationAttachingClientCall<ReqT, RespT>
            extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

        private final String        token;
        private static final String AUTHORIZATION_HEADER = "authorization";
        private static final String BASIC_PREFIX         = "Basic ";

        private AuthenticationAttachingClientCall(ClientCall<ReqT, RespT> delegate, String token) {
            super(delegate);
            this.token = token;
        }

        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
            headers.put(Metadata.Key.of(AUTHORIZATION_HEADER, Metadata.ASCII_STRING_MARSHALLER), BASIC_PREFIX + token);
            super.start(responseListener, headers);
        }
    }

}
