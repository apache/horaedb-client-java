/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.util;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * A stream-write buffer.
 *
 */
public interface StreamWriteBuf<V, R> {

    /**
     * Write data to buffer, but not flush to server.
     *
     * @param val data value
     * @return this
     */
    StreamWriteBuf<V, R> write(final V val);

    /**
     * @see #write(Object)
     */
    default StreamWriteBuf<V, R> write(final Collection<V> c) {
        c.forEach(this::write);
        return this;
    }

    /**
     * Flush buffer data to server.
     *
     * @return this
     */
    StreamWriteBuf<V, R> flush();

    /**
     * Flush the given data to the server directly. The default
     * implementation write data to the buffer first and then
     * flush the data to the server immediately.
     *
     * @param c collection of data
     * @return this
     */
    default StreamWriteBuf<V, R> writeAndFlush(final Collection<V> c) {
        return write(c).flush();
    }

    /**
     * Tell server that the stream-write has completed.
     *
     * @return the streaming-wrote future result
     */
    CompletableFuture<R> completed();
}
