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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceresdb.common.Endpoint;
import com.ceresdb.common.OptKeys;
import com.ceresdb.common.Tenant;
import com.ceresdb.common.util.AuthUtil;
import com.ceresdb.common.util.Clock;
import com.ceresdb.common.util.Cpus;
import com.ceresdb.common.util.ExecutorServiceHelper;
import com.ceresdb.common.util.MetricsUtil;
import com.ceresdb.common.util.NamedThreadFactory;
import com.ceresdb.common.util.ObjectPool;
import com.ceresdb.common.util.RefCell;
import com.ceresdb.common.util.Requires;
import com.ceresdb.common.util.SharedThreadPool;
import com.ceresdb.common.util.StringBuilderHelper;
import com.ceresdb.common.util.SystemPropertyUtil;
import com.ceresdb.common.util.ThreadPoolUtil;
import com.ceresdb.rpc.errors.ConnectFailException;
import com.ceresdb.rpc.errors.InvokeTimeoutException;
import com.ceresdb.rpc.errors.OnlyErrorMessage;
import com.ceresdb.rpc.errors.RemotingException;
import com.ceresdb.rpc.interceptors.AuthHeadersInterceptor;
import com.ceresdb.rpc.interceptors.ClientRequestLimitInterceptor;
import com.ceresdb.rpc.interceptors.ContextToHeadersInterceptor;
import com.ceresdb.rpc.interceptors.MetricInterceptor;
import com.ceresdb.rpc.limit.Gradient2Limit;
import com.ceresdb.rpc.limit.LimitMetricRegistry;
import com.ceresdb.rpc.limit.RequestLimiterBuilder;
import com.ceresdb.rpc.limit.VegasLimit;
import com.google.protobuf.Message;
import com.netflix.concurrency.limits.Limit;
import com.netflix.concurrency.limits.MetricRegistry;

/**
 * Grpc client implementation.
 *
 * @author jiachun.fjc
 */
public class GrpcClient implements RpcClient {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcClient.class);

    private static final SharedThreadPool SHARED_ASYNC_POOL = new SharedThreadPool(
            new ObjectPool.Resource<ExecutorService>() {

                @Override
                public ExecutorService create() {
                    return createDefaultRpcExecutor();
                }

                @Override
                public void close(final ExecutorService ins) {
                    ExecutorServiceHelper.shutdownAndAwaitTermination(ins);
                }
            });

    private static final int    CONN_RESET_THRESHOLD  = SystemPropertyUtil.getInt(OptKeys.GRPC_CONN_RESET_THRESHOLD, 3);
    private static final int    MAX_SIZE_TO_USE_ARRAY = 8192;
    private static final String LIMITER_NAME          = "grpc_call";
    private static final String EXECUTOR_NAME         = "grpc_executor";
    private static final String REQ_RT                = "req_rt";
    private static final String REQ_FAILED            = "req_failed";
    private static final String UNARY_CALL            = "unary-call";
    private static final String SERVER_STREAMING_CALL = "server-streaming-call";
    private static final String CLIENT_STREAMING_CALL = "client-streaming-call";

    private final Map<Endpoint, IdChannel>     managedChannelPool  = new ConcurrentHashMap<>();
    private final Map<Endpoint, AtomicInteger> transientFailures   = new ConcurrentHashMap<>();
    private final List<ClientInterceptor>      interceptors        = new CopyOnWriteArrayList<>();
    private final AtomicBoolean                started             = new AtomicBoolean(false);
    private final List<ConnectionObserver>     connectionObservers = new CopyOnWriteArrayList<>();
    private final MarshallerRegistry           marshallerRegistry;

    private String          tenant             = "none";
    private String          defaultChildTenant = "none";
    private RpcOptions      opts;
    private ExecutorService asyncPool;
    private boolean         useSharedAsyncPool;

    public GrpcClient(MarshallerRegistry marshallerRegistry) {
        this.marshallerRegistry = marshallerRegistry;
    }

    @Override
    public boolean init(final RpcOptions opts) {
        if (!this.started.compareAndSet(false, true)) {
            throw new IllegalStateException("GRPC client has started");
        }

        this.opts = Requires.requireNonNull(opts, "GrpcClient.opts").copy();

        if (this.opts.getRpcThreadPoolSize() > 0) {
            this.asyncPool = createRpcExecutor(this.opts);
            this.useSharedAsyncPool = false;
        } else {
            this.asyncPool = SHARED_ASYNC_POOL.getObject();
            this.useSharedAsyncPool = true;
        }

        initInterceptors();

        return true;
    }

    @Override
    public void shutdownGracefully() {
        if (!this.started.compareAndSet(true, false)) {
            return;
        }

        if (this.useSharedAsyncPool) {
            SHARED_ASYNC_POOL.returnObject(this.asyncPool);
        } else {
            ExecutorServiceHelper.shutdownAndAwaitTermination(this.asyncPool);
        }
        this.asyncPool = null;

        closeAllChannels();
    }

    @Override
    public boolean checkConnection(final Endpoint endpoint) {
        return checkConnection(endpoint, false);
    }

    @Override
    public boolean checkConnection(final Endpoint endpoint, final boolean createIfAbsent) {
        Requires.requireNonNull(endpoint, "endpoint");
        return checkChannel(endpoint, createIfAbsent);
    }

    @Override
    public void closeConnection(final Endpoint endpoint) {
        Requires.requireNonNull(endpoint, "endpoint");
        closeChannel(endpoint);
    }

    @Override
    public void registerConnectionObserver(final ConnectionObserver observer) {
        this.connectionObservers.add(observer);
    }

    @Override
    public <Req, Resp> Resp invokeSync(final Endpoint endpoint, //
                                       final Req request, //
                                       final Context ctx, //
                                       final long timeoutMs)
            throws RemotingException {
        final long timeout = calcTimeout(timeoutMs);
        final CompletableFuture<Resp> future = new CompletableFuture<>();

        invokeAsync(endpoint, request, ctx, new Observer<Resp>() {

            @Override
            public void onNext(final Resp value) {
                future.complete(value);
            }

            @Override
            public void onError(final Throwable err) {
                future.completeExceptionally(err);
            }
        }, timeout);

        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            future.cancel(true);
            throw new InvokeTimeoutException(e);
        } catch (final Throwable t) {
            future.cancel(true);
            throw new RemotingException(t);
        }
    }

    @Override
    public <Req, Resp> void invokeAsync(final Endpoint endpoint, //
                                        final Req request, //
                                        final Context ctx, //
                                        final Observer<Resp> observer, //
                                        final long timeoutMs) {
        checkArgs(endpoint, request, ctx, observer);

        final MethodDescriptor<Message, Message> method = getCallMethod(request, MethodDescriptor.MethodType.UNARY);
        final long timeout = calcTimeout(timeoutMs);
        final CallOptions callOpts = CallOptions.DEFAULT //
                .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS) //
                .withExecutor(getObserverExecutor(observer));

        final String childTenant = addChildTenantIntoCtx(ctx);
        final String methodName = method.getFullMethodName();
        final String address = endpoint.toString();
        final long startCall = Clock.defaultClock().getTick();

        final Channel ch = getCheckedChannel(endpoint, (err) -> {
            attachErrMsg(err, UNARY_CALL, methodName, childTenant, address, startCall, -1, ctx);
            observer.onError(err);
        });

        if (ch == null) {
            return;
        }

        final String target = target(ch, address);

        ClientCalls.asyncUnaryCall(ch.newCall(method, callOpts), (Message) request, new StreamObserver<Message>() {

            @SuppressWarnings("unchecked")
            @Override
            public void onNext(final Message value) {
                onReceived(false);
                observer.onNext((Resp) value);

            }

            @Override
            public void onError(final Throwable err) {
                attachErrMsg(err, UNARY_CALL, methodName, childTenant, target, startCall, onReceived(true), ctx);
                observer.onError(err);
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }

            private long onReceived(final boolean onError) {
                final long duration = Clock.defaultClock().duration(startCall);
                final String mthAndTnt = MetricsUtil.named(methodName, tenant);

                MetricsUtil.timer(REQ_RT, mthAndTnt).update(duration, TimeUnit.MILLISECONDS);
                MetricsUtil.timer(REQ_RT, mthAndTnt, address).update(duration, TimeUnit.MILLISECONDS);

                if (onError) {
                    MetricsUtil.meter(REQ_FAILED, mthAndTnt).mark();
                    MetricsUtil.meter(REQ_FAILED, mthAndTnt, address).mark();
                }

                return duration;
            }
        });
    }

    @Override
    public <Req, Resp> void invokeServerStreaming(final Endpoint endpoint, //
                                                  final Req request, //
                                                  final Context ctx, //
                                                  final Observer<Resp> observer) {
        checkArgs(endpoint, request, ctx, observer);

        final MethodDescriptor<Message, Message> method = getCallMethod(request,
                MethodDescriptor.MethodType.SERVER_STREAMING);
        final CallOptions callOpts = CallOptions.DEFAULT.withExecutor(getObserverExecutor(observer));

        final String childTenant = addChildTenantIntoCtx(ctx);
        final String methodName = method.getFullMethodName();
        final String address = endpoint.toString();
        final long startCall = Clock.defaultClock().getTick();

        final Channel ch = getCheckedChannel(endpoint, (err) -> {
            attachErrMsg(err, SERVER_STREAMING_CALL, methodName, childTenant, address, startCall, -1, ctx);
            observer.onError(err);
        });

        if (ch == null) {
            return;
        }

        final String target = target(ch, address);

        ClientCalls.asyncServerStreamingCall(ch.newCall(method, callOpts), (Message) request,
                new StreamObserver<Message>() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onNext(final Message value) {
                        observer.onNext((Resp) value);
                    }

                    @Override
                    public void onError(final Throwable err) {
                        attachErrMsg(err, SERVER_STREAMING_CALL, methodName, childTenant, target, startCall, -1, ctx);
                        observer.onError(err);
                    }

                    @Override
                    public void onCompleted() {
                        observer.onCompleted();
                    }
                });
    }

    @Override
    public <Req, Resp> Observer<Req> invokeClientStreaming(final Endpoint endpoint, //
                                                           final Req defaultReqIns, //
                                                           final Context ctx, //
                                                           final Observer<Resp> respObserver) {
        checkArgs(endpoint, defaultReqIns, ctx, respObserver);

        final MethodDescriptor<Message, Message> method = getCallMethod(defaultReqIns,
                MethodDescriptor.MethodType.CLIENT_STREAMING);
        final CallOptions callOpts = CallOptions.DEFAULT.withExecutor(getObserverExecutor(respObserver));

        final String childTenant = addChildTenantIntoCtx(ctx);
        final String methodName = method.getFullMethodName();
        final String address = endpoint.toString();
        final long startCall = Clock.defaultClock().getTick();

        final RefCell<Throwable> refErr = new RefCell<>();
        final Channel ch = getCheckedChannel(endpoint, (err) -> {
            attachErrMsg(err, CLIENT_STREAMING_CALL, methodName, childTenant, address, startCall, -1, ctx);
            refErr.set(err);
        });

        if (ch == null) {
            respObserver.onError(refErr.get());
            return new Observer.RejectedObserver<>(refErr.get());
        }

        final String target = target(ch, address);

        final StreamObserver<Message> gRpcObs = ClientCalls.asyncClientStreamingCall(ch.newCall(method, callOpts),
                new StreamObserver<Message>() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onNext(final Message value) {
                        respObserver.onNext((Resp) value);
                    }

                    @Override
                    public void onError(final Throwable err) {
                        attachErrMsg(err, CLIENT_STREAMING_CALL, methodName, childTenant, target, startCall, -1, ctx);
                        respObserver.onError(err);
                    }

                    @Override
                    public void onCompleted() {
                        respObserver.onCompleted();
                    }
                });

        return new Observer<Req>() {

            @Override
            public void onNext(final Req value) {
                gRpcObs.onNext((Message) value);
            }

            @Override
            public void onError(final Throwable err) {
                gRpcObs.onError(err);
            }

            @Override
            public void onCompleted() {
                gRpcObs.onCompleted();
            }
        };
    }

    public void addInterceptor(final ClientInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }

    // Interceptors run in the reverse order in which they are added
    private void initInterceptors() {
        // the last one
        addInterceptor(new MetricInterceptor());

        // the third
        final RpcOptions.LimitKind kind = this.opts.getLimitKind();
        if (kind != null && kind != RpcOptions.LimitKind.None) {
            addInterceptor(createRequestLimitInterceptor(kind));
        }

        // the second
        addInterceptor(new ContextToHeadersInterceptor());

        // the first
        final Tenant tenant = this.opts.getTenant();
        if (tenant != null) {
            this.tenant = tenant.getTenant();
            this.defaultChildTenant = tenant.getChildTenant();
            addInterceptor(new AuthHeadersInterceptor(tenant));
        }
    }

    private ClientRequestLimitInterceptor createRequestLimitInterceptor(final RpcOptions.LimitKind kind) {
        final MetricRegistry metricRegistry = new LimitMetricRegistry();

        final int minInitialLimit = 20;
        final Limit limit;
        switch (kind) {
            case Vegas:
                limit = VegasLimit.newBuilder() //
                        .initialLimit(Math.max(minInitialLimit, this.opts.getInitialLimit())) //
                        .maxConcurrency(this.opts.getMaxLimit()) //
                        .smoothing(this.opts.getSmoothing()) //
                        .logOnLimitChange(this.opts.isLogOnLimitChange()) //
                        .metricRegistry(metricRegistry) //
                        .build();
                break;
            case Gradient:
                limit = Gradient2Limit.newBuilder() //
                        .initialLimit(Math.max(minInitialLimit, this.opts.getInitialLimit())) //
                        .maxConcurrency(this.opts.getMaxLimit()) //
                        .longWindow(this.opts.getLongRttWindow()) //
                        .smoothing(this.opts.getSmoothing()) //
                        .queueSize(Math.max(4, Cpus.cpus() << 1)) //
                        .logOnLimitChange(this.opts.isLogOnLimitChange()) //
                        .metricRegistry(metricRegistry) //
                        .build();
                break;
            default:
                throw new IllegalArgumentException("Unsupported limit kind: " + kind);
        }

        final RequestLimiterBuilder limiterBuilder = RequestLimiterBuilder.newBuilder().named(LIMITER_NAME) //
                .metricRegistry(metricRegistry) //
                .blockOnLimit(this.opts.isBlockOnLimit(), this.opts.getDefaultRpcTimeout()) //
                .limit(limit);

        final Map<String, Double> methodsLimitPercent = this.marshallerRegistry.getAllMethodsLimitPercent();
        if (methodsLimitPercent.isEmpty()) {
            return new ClientRequestLimitInterceptor(limiterBuilder.build());
        } else {
            final double sum = methodsLimitPercent //
                    .values() //
                    .stream() //
                    .reduce(0.0, Double::sum);
            Requires.requireTrue(Math.abs(sum - 1.0) < 0.1, "the total percent sum of partitions must be near 100%");
            methodsLimitPercent.forEach(limiterBuilder::partition);

            return new ClientRequestLimitInterceptor(limiterBuilder.partitionByMethod().build(),
                    methodsLimitPercent::containsKey);
        }
    }

    private void attachErrMsg(final Throwable err, //
                              final String callType, //
                              final String method, //
                              final String childTenant, //
                              final String target, //
                              final long startCall, //
                              final long duration, //
                              final Context ctx) {
        final StringBuilder buf = StringBuilderHelper.get() //
                .append("GRPC ") //
                .append(callType) //
                .append(" got an error,") //
                .append(" method=") //
                .append(method) //
                .append(", tenant=") //
                .append(this.tenant) //
                .append(", childTenant=") //
                .append(childTenant != null ? childTenant : this.defaultChildTenant) //
                .append(", target=") //
                .append(target) //
                .append(", startCall=") //
                .append(startCall);
        if (duration > 0) {
            buf.append(", duration=") //
                    .append(duration) //
                    .append(" millis");
        }
        buf.append(", ctx=") //
                .append(ctx);
        err.addSuppressed(new OnlyErrorMessage(buf.toString()));
    }

    private long calcTimeout(final long timeoutMs) {
        return timeoutMs > 0 ? timeoutMs : this.opts.getDefaultRpcTimeout();
    }

    private Executor getObserverExecutor(final Observer<?> observer) {
        return observer.executor() != null ? observer.executor() : this.asyncPool;
    }

    private String addChildTenantIntoCtx(final Context ctx) {
        final String childTenant = ctx.remove(AuthUtil.HEAD_ACCESS_CHILD_TENANT);
        // null value means clear previous
        AuthHeadersInterceptor.setCurrentChildTenant(childTenant);
        ContextToHeadersInterceptor.setCurrentCtx(ctx);
        return childTenant;
    }

    private void closeAllChannels() {
        this.managedChannelPool.values().forEach(ch -> {
            final boolean ret = ManagedChannelHelper.shutdownAndAwaitTermination(ch);
            LOG.info("Shutdown managed channel: {}, {}.", ch, ret ? "success" : "failed");
        });
        this.managedChannelPool.clear();
    }

    private void closeChannel(final Endpoint endpoint) {
        final ManagedChannel ch = this.managedChannelPool.remove(endpoint);
        LOG.info("Close connection: {}, {}.", endpoint, ch);
        if (ch != null) {
            ManagedChannelHelper.shutdownAndAwaitTermination(ch);
        }
    }

    private boolean checkChannel(final Endpoint endpoint, final boolean createIfAbsent) {
        final ManagedChannel ch = getChannel(endpoint, createIfAbsent);

        if (ch == null) {
            return false;
        }

        return checkConnectivity(endpoint, ch);
    }

    private boolean checkConnectivity(final Endpoint endpoint, final ManagedChannel ch) {
        final ConnectivityState st = ch.getState(false);

        if (st != ConnectivityState.TRANSIENT_FAILURE && st != ConnectivityState.SHUTDOWN) {
            return true;
        }

        final int c = incConnFailuresCount(endpoint);
        if (c < CONN_RESET_THRESHOLD) {
            if (c == CONN_RESET_THRESHOLD - 1) {
                // For sub-channels that are in TRANSIENT_FAILURE state, short-circuit the backoff timer and make
                // them reconnect immediately. May also attempt to invoke NameResolver#refresh
                ch.resetConnectBackoff();
            }
            return true;
        }

        clearConnFailuresCount(endpoint);

        final IdChannel removedCh = this.managedChannelPool.remove(endpoint);

        if (removedCh == null) {
            // The channel has been removed and closed by another
            return false;
        }

        LOG.warn("Channel {} in [INACTIVE] state {} times, it has been removed from the pool.",
                target(removedCh, endpoint), c);

        if (removedCh != ch) {
            // Now that it's removed, close it
            ManagedChannelHelper.shutdownAndAwaitTermination(removedCh, 100);
        }

        ManagedChannelHelper.shutdownAndAwaitTermination(ch, 100);

        return false;
    }

    private int incConnFailuresCount(final Endpoint endpoint) {
        return this.transientFailures.computeIfAbsent(endpoint, ep -> new AtomicInteger()).incrementAndGet();
    }

    private void clearConnFailuresCount(final Endpoint endpoint) {
        this.transientFailures.remove(endpoint);
    }

    private MethodDescriptor<Message, Message> getCallMethod(final Object request, //
                                                             final MethodDescriptor.MethodType methodType) {
        Requires.requireTrue(request instanceof Message, "gRPC impl only support protobuf");
        final Class<? extends Message> reqCls = ((Message) request).getClass();
        final Message defaultReqIns = this.marshallerRegistry.getDefaultRequestInstance(reqCls);
        final Message defaultRespIns = this.marshallerRegistry.getDefaultResponseInstance(reqCls);
        Requires.requireNonNull(defaultReqIns, "null default request instance: " + reqCls.getName());
        Requires.requireNonNull(defaultRespIns, "null default response instance: " + reqCls.getName());

        return MethodDescriptor //
                .<Message, Message> newBuilder() //
                .setType(methodType) //
                .setFullMethodName(this.marshallerRegistry.getMethodName(reqCls, methodType)) //
                .setRequestMarshaller(ProtoUtils.marshaller(defaultReqIns)) //
                .setResponseMarshaller(ProtoUtils.marshaller(defaultRespIns)) //
                .build();
    }

    private Channel getCheckedChannel(final Endpoint endpoint, final Consumer<Throwable> onFailed) {
        final ManagedChannel ch = getChannel(endpoint, true);

        if (checkConnectivity(endpoint, ch)) {
            return ch;
        }

        onFailed.accept(new ConnectFailException("Connect failed to " + endpoint));

        return null;
    }

    private ManagedChannel getChannel(final Endpoint endpoint, final boolean createIfAbsent) {
        if (createIfAbsent) {
            return this.managedChannelPool.computeIfAbsent(endpoint, this::newChannel);
        } else {
            return this.managedChannelPool.get(endpoint);
        }
    }

    private IdChannel newChannel(final Endpoint endpoint) {
        final ManagedChannel innerChannel = NettyChannelBuilder.forAddress(endpoint.getIp(), endpoint.getPort()) //
                .usePlaintext() //
                .executor(this.asyncPool) //
                .intercept(this.interceptors) //
                .maxInboundMessageSize(this.opts.getMaxInboundMessageSize()) //
                .flowControlWindow(this.opts.getFlowControlWindow()) //
                .idleTimeout(this.opts.getIdleTimeoutSeconds(), TimeUnit.SECONDS) //
                .keepAliveTime(this.opts.getKeepAliveTimeSeconds(), TimeUnit.SECONDS) //
                .keepAliveTimeout(this.opts.getKeepAliveTimeoutSeconds(), TimeUnit.SECONDS) //
                .keepAliveWithoutCalls(this.opts.isKeepAliveWithoutCalls()) //
                .withOption(ChannelOption.SO_REUSEADDR, true) //
                .withOption(ChannelOption.TCP_NODELAY, true) //
                .build();

        final IdChannel idChannel = new IdChannel(innerChannel);

        if (LOG.isInfoEnabled()) {
            LOG.info("Creating new channel to: {}.", target(idChannel, endpoint));
        }

        // The init channel state is IDLE
        notifyWhenStateChanged(ConnectivityState.IDLE, endpoint, idChannel);

        return idChannel;
    }

    private void notifyWhenStateChanged(final ConnectivityState state, final Endpoint endpoint, final IdChannel ch) {
        ch.notifyWhenStateChanged(state, () -> onStateChanged(endpoint, ch));
    }

    private void onStateChanged(final Endpoint endpoint, final IdChannel ch) {
        final ConnectivityState state = ch.getState(false);

        if (LOG.isInfoEnabled()) {
            LOG.info("The channel {} is in state: {}.", target(ch, endpoint), state);
        }

        switch (state) {
            case READY:
                notifyReady(endpoint);
                notifyWhenStateChanged(ConnectivityState.READY, endpoint, ch);
                break;
            case TRANSIENT_FAILURE:
                notifyFailure(endpoint);
                notifyWhenStateChanged(ConnectivityState.TRANSIENT_FAILURE, endpoint, ch);
                break;
            case SHUTDOWN:
                notifyShutdown(endpoint);
                break;
            case CONNECTING:
                notifyWhenStateChanged(ConnectivityState.CONNECTING, endpoint, ch);
                break;
            case IDLE:
                notifyWhenStateChanged(ConnectivityState.IDLE, endpoint, ch);
                break;
        }
    }

    private void notifyReady(final Endpoint endpoint) {
        this.connectionObservers.forEach(o -> o.onReady(endpoint));
    }

    private void notifyFailure(final Endpoint endpoint) {
        this.connectionObservers.forEach(o -> o.onFailure(endpoint));
    }

    private void notifyShutdown(final Endpoint endpoint) {
        this.connectionObservers.forEach(o -> o.onShutdown(endpoint));
    }

    @Override
    public void display(final Printer out) {
        out.println("--- GrpcClient ---")//
                .print("started=") //
                .println(this.started) //
                .print("opts=") //
                .println(this.opts) //
                .print("connectionObservers=") //
                .println(this.connectionObservers) //
                .print("asyncPool=") //
                .println(this.asyncPool) //
                .print("interceptors=") //
                .println(this.interceptors) //
                .print("managedChannelPool=") //
                .println(this.managedChannelPool) //
                .print("transientFailures=") //
                .println(this.transientFailures);
    }

    private static String target(final Channel ch, final Endpoint ep) {
        return target(ch, ep == null ? null : ep.toString());
    }

    private static String target(final Channel ch, final String address) {
        return StringBuilderHelper.get() //
                .append('[') //
                .append(channelId(ch)) //
                .append('/') //
                .append(address) //
                .append(']') //
                .toString();
    }

    private static long channelId(final Channel ch) {
        if (ch instanceof IdChannel) {
            return ((IdChannel) ch).getChannelId();
        }
        return -1;
    }

    private static void checkArgs(final Endpoint endpoint, //
                                  final Object request, //
                                  final Context ctx, //
                                  final Observer<?> observer) {
        Requires.requireNonNull(endpoint, "endpoint");
        Requires.requireNonNull(request, "request");
        Requires.requireNonNull(ctx, "ctx");
        Requires.requireNonNull(observer, "observer");
    }

    private static ExecutorService createRpcExecutor(final RpcOptions opts) {
        final int workQueueSize = opts.getRpcThreadPoolQueueSize();
        final BlockingQueue<Runnable> workQueue;
        if (workQueueSize <= 0) {
            workQueue = new SynchronousQueue<>();
        } else if (workQueueSize <= MAX_SIZE_TO_USE_ARRAY) {
            workQueue = new ArrayBlockingQueue<>(workQueueSize);
        } else {
            workQueue = new LinkedBlockingQueue<>(workQueueSize);
        }

        return ThreadPoolUtil.newBuilder() //
                .poolName(EXECUTOR_NAME) //
                .enableMetric(true) //
                .coreThreads(Math.min(Cpus.cpus(), opts.getRpcThreadPoolSize())) //
                .maximumThreads(opts.getRpcThreadPoolSize()) //
                .keepAliveSeconds(60L) //
                .workQueue(workQueue) //
                .threadFactory(new NamedThreadFactory(EXECUTOR_NAME, true)) //
                .rejectedHandler(new AsyncPoolRejectedHandler(EXECUTOR_NAME)) //
                .build();
    }

    private static ExecutorService createDefaultRpcExecutor() {
        final String name = "default_shared_" + EXECUTOR_NAME;
        return ThreadPoolUtil.newBuilder() //
                .poolName(name) //
                .enableMetric(true) //
                .coreThreads(Cpus.cpus()) //
                .maximumThreads(Cpus.cpus() << 2) //
                .keepAliveSeconds(60L) //
                .workQueue(new ArrayBlockingQueue<>(512)) //
                .threadFactory(new NamedThreadFactory(name, true)) //
                .rejectedHandler(new AsyncPoolRejectedHandler(name)) //
                .build();
    }

    private static class AsyncPoolRejectedHandler implements RejectedExecutionHandler {

        private final String name;

        AsyncPoolRejectedHandler(String name) {
            this.name = name;
        }

        @Override
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            LOG.error("Thread poll {} is busy, the caller thread {} will run this task {}.", this.name,
                    Thread.currentThread(), r);
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}
