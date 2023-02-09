/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb;

import java.util.concurrent.CompletableFuture;

import io.ceresdb.models.Err;
import io.ceresdb.models.Point;
import io.ceresdb.models.Result;
import io.ceresdb.models.WriteOk;
import io.ceresdb.models.WriteRequest;
import io.ceresdb.rpc.Context;
import io.ceresdb.util.StreamWriteBuf;

/**
 * CeresDB write API. Writes the streaming data to the database, support
 * failed retries.
 *
 */
public interface Write {

    /**
     * @see #write(WriteRequest, Context)
     */
    default CompletableFuture<Result<WriteOk, Err>> write(final WriteRequest req) {
        return write(req, Context.newDefault());
    }

    /**
     * Write the data stream to the database.
     *
     * @param req write request
     * @param ctx the invoked context
     * @return write result
     */
    CompletableFuture<Result<WriteOk, Err>> write(final WriteRequest req, final Context ctx);

    /**
     * @see #streamWrite(String, Context)
     */
    default StreamWriteBuf<Point, WriteOk> streamWrite(final String table) {
        return streamWrite(table, Context.newDefault());
    }

    /**
     * Executes a stream-write-call, returns a write request observer for streaming-write.
     *
     * @param table  the table to write
     * @param ctx    the invoked context
     * @return a write request observer for streaming-write
     */
    StreamWriteBuf<Point, WriteOk> streamWrite(final String table, final Context ctx);
}
