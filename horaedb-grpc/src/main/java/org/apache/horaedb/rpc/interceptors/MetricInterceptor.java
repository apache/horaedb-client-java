/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc.interceptors;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import org.apache.horaedb.common.util.MetricsUtil;
import com.codahale.metrics.Counter;
import com.google.protobuf.MessageLite;

/**
 * Request method metric interceptor.
 *
 */
public class MetricInterceptor implements ClientInterceptor {

    private static final String REQ_TYPE  = "req";
    private static final String RESP_TYPE = "resp";

    private static final String QPS              = "qps";
    private static final String BYTES            = "bytes";
    private static final String SERIALIZED_BYTES = "serialized_bytes";

    private static final Counter REQ_BYTES  = MetricsUtil.counter(REQ_TYPE, BYTES);
    private static final Counter RESP_BYTES = MetricsUtil.counter(RESP_TYPE, BYTES);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method, //
                                                               final CallOptions callOpts, //
                                                               final Channel next) {
        final String methodName = method.getFullMethodName();
        MetricsUtil.meter(REQ_TYPE, QPS, methodName).mark();

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOpts)) {

            @Override
            public void start(final Listener<RespT> respListener, final Metadata headers) {
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(respListener) {

                    @Override
                    public void onMessage(final RespT msg) {
                        if (msg instanceof MessageLite) {
                            final int size = ((MessageLite) msg).getSerializedSize();
                            MetricsUtil.histogram(RESP_TYPE, SERIALIZED_BYTES, methodName).update(size);
                            RESP_BYTES.inc(size);
                        }
                        super.onMessage(msg);
                    }
                }, headers);
            }

            @Override
            public void sendMessage(final ReqT msg) {
                if (msg instanceof MessageLite) {
                    final int size = ((MessageLite) msg).getSerializedSize();
                    MetricsUtil.histogram(REQ_TYPE, SERIALIZED_BYTES, methodName).update(size);
                    REQ_BYTES.inc(size);
                }
                super.sendMessage(msg);
            }
        };
    }
}
