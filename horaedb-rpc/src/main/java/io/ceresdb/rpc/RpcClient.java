/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.rpc;

import org.apache.horaedb.common.Display;
import org.apache.horaedb.common.Endpoint;
import org.apache.horaedb.common.Lifecycle;
import io.ceresdb.rpc.errors.RemotingException;

/**
 * A common RPC client.
 *
 */
public interface RpcClient extends Lifecycle<RpcOptions>, Display {

    /**
     * Check connection for given address.
     *
     * @param endpoint target address
     * @return true if there is a connection adn the connection is active adn writable.
     */
    boolean checkConnection(final Endpoint endpoint);

    /**
     * Check connection for given address and async to create a new one if there is no connection.
     * @param endpoint       target address
     * @param createIfAbsent create a new one if there is no connection
     * @return true if there is a connection and the connection is active and writable.
     */
    boolean checkConnection(final Endpoint endpoint, final boolean createIfAbsent);

    /**
     * Close all connections of a address.
     *
     * @param endpoint target address
     */
    void closeConnection(final Endpoint endpoint);

    /**
     * Register a connection state observer.
     *
     * @param observer connection state observer
     */
    void registerConnectionObserver(final ConnectionObserver observer);

    /**
     * A connection observer.
     */
    interface ConnectionObserver {

        /**
         * The channel has successfully established a connection.
         *
         * @param ep the server endpoint
         */
        void onReady(final Endpoint ep);

        /**
         * There has been some transient failure (such as a TCP 3-way handshake timing
         * out or a socket error).
         *
         * @param ep the server endpoint
         */
        void onFailure(final Endpoint ep);

        /**
         * This channel has started shutting down. Any new RPCs should fail immediately.
         *
         * @param ep the server endpoint
         */
        void onShutdown(final Endpoint ep);
    }

    /**
     * Executes a synchronous call.
     *
     * @param endpoint  target address
     * @param request   request object
     * @param timeoutMs timeout millisecond
     * @param <Req>     request message type
     * @param <Resp>    response message type
     * @return response
     */
    default <Req, Resp> Resp invokeSync(final Endpoint endpoint, //
                                        final Req request, //
                                        final long timeoutMs)
            throws RemotingException {
        return invokeSync(endpoint, request, null, timeoutMs);
    }

    /**
     * Executes a synchronous call using a invoke context.
     *
     * @param endpoint  target address
     * @param request   request object
     * @param ctx       invoke context
     * @param timeoutMs timeout millisecond
     * @param <Req>     request message type
     * @param <Resp>    response message type
     * @return response
     */
    <Req, Resp> Resp invokeSync(final Endpoint endpoint, //
                                final Req request, //
                                final Context ctx, //
                                final long timeoutMs)
            throws RemotingException;

    /**
     * Executes a asynchronous call with a response {@link Observer}.
     *
     * @param endpoint  target address
     * @param request   request object
     * @param observer  response observer
     * @param timeoutMs timeout millisecond
     * @param <Req>     request message type
     * @param <Resp>    response message type
     */
    default <Req, Resp> void invokeAsync(final Endpoint endpoint, //
                                         final Req request, //
                                         final Observer<Resp> observer, //
                                         final long timeoutMs)
            throws RemotingException {
        invokeAsync(endpoint, request, null, observer, timeoutMs);
    }

    /**
     * Executes a asynchronous call with a response {@link Observer}.
     *
     * @param endpoint  target address
     * @param request   request object
     * @param ctx       invoke context
     * @param observer  response observer
     * @param timeoutMs timeout millisecond
     * @param <Req>     request message type
     * @param <Resp>    response message type
     */
    <Req, Resp> void invokeAsync(final Endpoint endpoint, //
                                 final Req request, //
                                 final Context ctx, //
                                 final Observer<Resp> observer, //
                                 final long timeoutMs)
            throws RemotingException;

    /**
     * Executes a server-streaming call with a response {@link Observer}.
     *
     * One request message followed by zero or more response messages.
     *
     * @param endpoint target address
     * @param request  request object
     * @param ctx      invoke context
     * @param observer response stream observer
     * @param <Req>    request message type
     * @param <Resp>   response message type
     */
    <Req, Resp> void invokeServerStreaming(final Endpoint endpoint, //
                                           final Req request, //
                                           final Context ctx, //
                                           final Observer<Resp> observer)
            throws RemotingException;

    /**
     * Executes a client-streaming call with a request {@link Observer}
     * and a response {@link Observer}.
     *
     * @param endpoint      target address
     * @param defaultReqIns the default request instance
     * @param ctx           invoke context
     * @param respObserver  response stream observer
     * @param <Req>         request message type
     * @param <Resp>        response message type
     * @return request {@link Observer}.
     */
    <Req, Resp> Observer<Req> invokeClientStreaming(final Endpoint endpoint, final Req defaultReqIns, final Context ctx,
                                                    final Observer<Resp> respObserver)
            throws RemotingException;
}
