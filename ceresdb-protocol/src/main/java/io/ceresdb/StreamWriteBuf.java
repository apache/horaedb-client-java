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
package io.ceresdb;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * A stream-write buffer.
 *
 * @author jiachun.fjc
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
