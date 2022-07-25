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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;

/**
 * A managed channel that has a channel id.
 *
 * @author jiachun.fjc
 */
public class IdChannel extends ManagedChannel {

    private static final AtomicLong ID_ALLOC = new AtomicLong();

    private final long           channelId;
    private final ManagedChannel channel;

    private static long getNextId() {
        return ID_ALLOC.incrementAndGet();
    }

    public IdChannel(ManagedChannel channel) {
        this.channelId = getNextId();
        this.channel = channel;
    }

    public long getChannelId() {
        return channelId;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    @Override
    public ManagedChannel shutdown() {
        return this.channel.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return this.channel.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.channel.isTerminated();
    }

    @Override
    public ManagedChannel shutdownNow() {
        return this.channel.shutdownNow();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return this.channel.awaitTermination(timeout, unit);
    }

    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(final MethodDescriptor<RequestT, ResponseT> methodDescriptor,
                                                                         final CallOptions callOptions) {
        return this.channel.newCall(methodDescriptor, callOptions);
    }

    @Override
    public String authority() {
        return this.channel.authority();
    }

    @Override
    public ConnectivityState getState(final boolean requestConnection) {
        return this.channel.getState(requestConnection);
    }

    @Override
    public void notifyWhenStateChanged(final ConnectivityState source, final Runnable callback) {
        this.channel.notifyWhenStateChanged(source, callback);
    }

    @Override
    public void resetConnectBackoff() {
        this.channel.resetConnectBackoff();
    }

    @Override
    public void enterIdle() {
        this.channel.enterIdle();
    }

    @Override
    public String toString() {
        return "IdChannel{" + //
               "channelId=" + channelId + //
               ", channel=" + channel + //
               '}';
    }
}
