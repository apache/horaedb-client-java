/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.horaedb.errors.IteratorException;
import org.apache.horaedb.models.SqlQueryOk;
import org.apache.horaedb.models.Row;
import org.apache.horaedb.rpc.Observer;

/**
 * A blocking iterator, the `hasNext` method will be blocked until
 * the server returns data or the process ends.
 *
 */
public class BlockingStreamIterator implements Iterator<Stream<Row>> {

    private static final SqlQueryOk EOF = SqlQueryOk.emptyOk();

    private final long     timeout;
    private final TimeUnit unit;

    private final BlockingQueue<Object> staging = new LinkedBlockingQueue<>();
    private final Observer<SqlQueryOk>  observer;
    private SqlQueryOk                  next;

    public BlockingStreamIterator(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
        this.observer = new Observer<SqlQueryOk>() {

            @Override
            public void onNext(final SqlQueryOk value) {
                staging.offer(value);
            }

            @Override
            public void onError(final Throwable err) {
                staging.offer(err);
            }

            @Override
            public void onCompleted() {
                staging.offer(EOF);
            }
        };
    }

    @Override
    public boolean hasNext() {
        if (this.next == EOF) {
            return false;
        }

        try {
            final Object v = this.staging.poll(this.timeout, this.unit);

            if (v == null) {
                return reject("Stream iterator timeout");
            }

            if (v instanceof Throwable) {
                return reject("Stream iterator got an error", (Throwable) v);
            }

            this.next = (SqlQueryOk) v;

            return this.next != EOF;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return reject("Interrupted", e);
        }
    }

    @Override
    public Stream<Row> next() {
        if (this.next == null) {
            return reject("Null `next` element");
        }

        if (this.next == EOF) {
            return reject("Reaches the end of the iterator");
        }

        return this.next.stream();
    }

    public Observer<SqlQueryOk> getObserver() {
        return this.observer;
    }

    private static <T> T reject(final String msg) {
        throw new IteratorException(msg);
    }

    private static <T> T reject(final String msg, final Throwable cause) {
        throw new IteratorException(msg, cause);
    }
}
