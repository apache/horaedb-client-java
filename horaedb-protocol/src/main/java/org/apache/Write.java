/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache;

import java.util.concurrent.CompletableFuture;

import org.apache.horaedb.models.Err;
import org.apache.horaedb.models.Point;
import org.apache.horaedb.models.RequestContext;
import org.apache.horaedb.models.Result;
import org.apache.horaedb.models.WriteOk;
import org.apache.horaedb.models.WriteRequest;
import org.apache.horaedb.rpc.Context;
import org.apache.horaedb.util.StreamWriteBuf;

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
     * @see #streamWrite(RequestContext, String, Context)
     */
    default StreamWriteBuf<Point, WriteOk> streamWrite(final String table) {
        return streamWrite(null, table, Context.newDefault());
    }

    default StreamWriteBuf<Point, WriteOk> streamWrite(final RequestContext reqCtx, final String table) {
        return streamWrite(reqCtx, table, Context.newDefault());
    }

    /**
     * Executes a stream-write-call, returns a write request observer for streaming-write.
     *
     * @param table  the table to write
     * @param ctx    the invoked context
     * @return a write request observer for streaming-write
     */
    StreamWriteBuf<Point, WriteOk> streamWrite(RequestContext reqCtx, final String table, final Context ctx);
}
