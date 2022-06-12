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
package com.ceresdb;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.ceresdb.models.Err;
import com.ceresdb.models.Result;
import com.ceresdb.models.Rows;
import com.ceresdb.models.WriteOk;
import com.ceresdb.rpc.Context;

/**
 * CeresDB write API. Writes the streaming data to the database, support
 * failed retries.
 *
 * @author jiachun.fjc
 */
public interface Write {

    /**
     * @see #write(Rows, Context)
     */
    default CompletableFuture<Result<WriteOk, Err>> write(final Rows data) {
        return write(Collections.singletonList(data), Context.newDefault());
    }

    /**
     * Write a single series data to database.
     *
     * @param data rows
     * @param ctx  the invoke context
     * @return write result
     */
    default CompletableFuture<Result<WriteOk, Err>> write(final Rows data, final Context ctx) {
        return write(Collections.singletonList(data), ctx);
    }

    /**
     * @see #write(Collection, Context)
     */
    default CompletableFuture<Result<WriteOk, Err>> write(final Collection<Rows> data) {
        return write(data, Context.newDefault());
    }

    /**
     * Write the data stream to the database.
     *
     * @param data rows
     * @param ctx  the invoke context
     * @return write result
     */
    CompletableFuture<Result<WriteOk, Err>> write(final Collection<Rows> data, final Context ctx);

    /**
     * @see #streamWrite(String, Context)
     */
    default StreamWriteBuf<Rows, WriteOk> streamWrite(final String metric) {
        return streamWrite(metric, Context.newDefault());
    }

    /**
     * Executes a stream-write-call, returns a write request observer for streaming-write.
     *
     * @param metric the metric to write
     * @param ctx    the invoke context
     * @return a write request observer for streaming-write
     */
    StreamWriteBuf<Rows, WriteOk> streamWrite(final String metric, final Context ctx);
}
