/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.rpc;

import java.util.concurrent.Executor;

import org.apache.horaedb.common.util.internal.ThrowUtil;

/**
 * Receives notifications from an observable stream of messages.
 *
 */
public interface Observer<V> {

    /**
     * Receives a value from the stream.
     *
     * <p>Can be called many times but is never called after {@link #onError(Throwable)}
     * or {@link #onCompleted()} are called.
     *
     * @param value the value passed to the stream
     */
    void onNext(final V value);

    /**
     * Receives a terminating error from the stream.
     *
     * <p>May only be called once and if called it must be the last method called. In
     * particular if an exception is thrown by an implementation of {@code onError}
     * no further calls to any method are allowed.
     *
     * @param err the error occurred on the stream
     */
    void onError(final Throwable err);

    /**
     * Receives a notification of successful stream completion.
     *
     * <p>May only be called once and if called it must be the last method called. In
     * particular if an exception is thrown by an implementation of {@code onCompleted}
     * no further calls to any method are allowed.
     */
    default void onCompleted() {
        // NO-OP
    }

    default Executor executor() {
        return null;
    }

    class RejectedObserver<V> implements Observer<V> {

        private final Throwable err;

        public RejectedObserver(Throwable err) {
            this.err = err;
        }

        @Override
        public void onNext(final V value) {
            reject();
        }

        @Override
        public void onError(final Throwable err) {
            reject();
        }

        @Override
        public void onCompleted() {
            reject();
        }

        private void reject() {
            ThrowUtil.throwException(this.err);
        }
    }
}
