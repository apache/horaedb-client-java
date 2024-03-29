/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.rpc.interceptors;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import org.apache.horaedb.common.util.MetricsUtil;
import io.ceresdb.rpc.limit.LimitMetricRegistry;
import io.ceresdb.rpc.limit.RequestLimitCtx;
import com.netflix.concurrency.limits.Limiter;

/**
 * ClientInterceptor that enforces per service and/or per method concurrent
 * request limits and returns a Status.UNAVAILABLE when that limit has been
 * reached.
 *
 * Refer to `concurrency-limit-grpc`
 *
 */
public class ClientRequestLimitInterceptor implements ClientInterceptor {

    private static final Status LIMIT_EXCEEDED_STATUS = Status.UNAVAILABLE.withDescription("Client limit reached");

    private static final AtomicBoolean LIMIT_SWITCH = new AtomicBoolean(true);

    private final Limiter<RequestLimitCtx>  limiter;
    private final Function<String, Boolean> filter;

    public ClientRequestLimitInterceptor(Limiter<RequestLimitCtx> limiter) {
        this(limiter, (name) -> true);
    }

    public ClientRequestLimitInterceptor(Limiter<RequestLimitCtx> limiter, Function<String, Boolean> filter) {
        this.limiter = limiter;
        this.filter = filter;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method, //
                                                               final CallOptions callOpts, //
                                                               final Channel next) {
        if (shouldNotUseLimiter(method.getType()) || !this.filter.apply(method.getFullMethodName())) {
            return next.newCall(method, callOpts);
        }

        final String methodName = method.getFullMethodName();

        return MetricsUtil.timer(LimitMetricRegistry.RPC_LIMITER, "acquire_time", methodName)
                .timeSupplier(() -> this.limiter.acquire(() -> methodName))
                .map(listener -> (ClientCall<ReqT, RespT>) new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                        next.newCall(method, callOpts)) {

                    private final AtomicBoolean done = new AtomicBoolean(false);

                    @Override
                    public void start(final Listener<RespT> respListener, final Metadata headers) {
                        super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                                respListener) {

                            @Override
                            public void onClose(final Status status, final Metadata trailers) {
                                try {
                                    super.onClose(status, trailers);
                                } finally {
                                    if (done.compareAndSet(false, true)) {
                                        if (status.isOk()) {
                                            listener.onSuccess();
                                        } else if (Status.Code.UNAVAILABLE == status.getCode()) {
                                            listener.onDropped();
                                        } else {
                                            listener.onIgnore();
                                        }
                                    }
                                }
                            }
                        }, headers);
                    }

                    @Override
                    public void cancel(final String message, final Throwable cause) {
                        try {
                            super.cancel(message, cause);
                        } finally {
                            if (done.compareAndSet(false, true)) {
                                listener.onIgnore();
                            }
                        }
                    }
                }).orElseGet(() -> new ClientCall<ReqT, RespT>() {

                    private Listener<RespT> respListener;

                    @Override
                    public void start(final Listener<RespT> respListener, final Metadata headers) {
                        this.respListener = respListener;
                    }

                    @Override
                    public void request(final int numMessages) {
                    }

                    @Override
                    public void cancel(final String message, final Throwable cause) {
                    }

                    @Override
                    public void halfClose() {
                        this.respListener.onClose(LIMIT_EXCEEDED_STATUS, new Metadata());
                    }

                    @Override
                    public void sendMessage(final ReqT message) {
                    }
                });
    }

    /**
     * Whether limit open.
     *
     * @return true or false
     */
    public static boolean isLimitSwitchOpen() {
        return LIMIT_SWITCH.get();
    }

    /**
     * See {@link #isLimitSwitchOpen()}
     *
     * Reset `limitSwitch`, set to the opposite of the old value.
     *
     * @return old value
     */
    public static boolean resetLimitSwitch() {
        return LIMIT_SWITCH.getAndSet(!LIMIT_SWITCH.get());
    }

    private static boolean shouldNotUseLimiter(final MethodDescriptor.MethodType methodType) {
        if (!isLimitSwitchOpen()) {
            return true;
        }

        return methodType != MethodDescriptor.MethodType.UNARY
               && methodType != MethodDescriptor.MethodType.BIDI_STREAMING;
    }
}
