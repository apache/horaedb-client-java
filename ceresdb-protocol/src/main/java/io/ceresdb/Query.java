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

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.ceresdb.models.Err;
import io.ceresdb.models.QueryOk;
import io.ceresdb.models.QueryRequest;
import io.ceresdb.models.Record;
import io.ceresdb.models.Result;
import io.ceresdb.rpc.Context;
import io.ceresdb.rpc.Observer;

/**
 * The query API for CeresDB client.
 *
 * @author jiachun.fjc
 */
public interface Query {

    /**
     * @see #query(QueryRequest, Context)
     */
    default CompletableFuture<Result<QueryOk, Err>> query(final QueryRequest req) {
        return query(req, Context.newDefault());
    }

    /**
     * According to the conditions, query data from the database.
     *
     * @param req the query request
     * @param ctx the invoke context
     * @return query result
     */
    CompletableFuture<Result<QueryOk, Err>> query(final QueryRequest req, final Context ctx);

    /**
     * @see #streamQuery(QueryRequest, Context, Observer)
     */
    default void streamQuery(final QueryRequest req, final Observer<QueryOk> observer) {
        streamQuery(req, Context.newDefault(), observer);
    }

    /**
     * Executes a stream-query-call with a streaming response.
     *
     * @param req      the query request
     * @param observer receives data from an observable stream
     * @param ctx      the invoke context
     */
    void streamQuery(final QueryRequest req, final Context ctx, final Observer<QueryOk> observer);

    /**
     * @see #blockingStreamQuery(QueryRequest, long, TimeUnit, Context)
     */
    default Iterator<Record> blockingStreamQuery(final QueryRequest req, //
                                                 final long timeout, //
                                                 final TimeUnit unit) {
        return blockingStreamQuery(req, timeout, unit, Context.newDefault());
    }

    /**
     * Executes a stream-query-call with a streaming response.
     *
     * @param req     the query request
     * @param timeout how long to wait {@link Iterator#hasNext()} before giving up, in units of unit
     * @param unit    a TimeUnit determining how to interpret the timeout parameter
     * @param ctx     the invoke context
     * @return the iterator of record
     */
    default Iterator<Record> blockingStreamQuery(final QueryRequest req, //
                                                 final long timeout, //
                                                 final TimeUnit unit, //
                                                 final Context ctx) {
        final BlockingStreamIterator streams = new BlockingStreamIterator(timeout, unit);
        streamQuery(req, ctx, streams.getObserver());
        return new RecordIterator(streams);
    }
}
