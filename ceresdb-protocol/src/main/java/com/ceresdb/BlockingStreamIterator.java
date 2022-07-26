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

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.ceresdb.errors.IteratorException;
import com.ceresdb.models.QueryOk;
import com.ceresdb.models.Record;
import com.ceresdb.rpc.Observer;

/**
 * A blocking iterator, the `hasNext` method will be blocked until
 * the server returns data or the process ends.
 *
 * @author jiachun.fjc
 */
public class BlockingStreamIterator implements Iterator<Stream<Record>> {

    private static final QueryOk EOF = QueryOk.emptyOk();

    private final long     timeout;
    private final TimeUnit unit;

    private final BlockingQueue<Object> staging = new LinkedBlockingQueue<>();
    private final Observer<QueryOk>     observer;
    private QueryOk                     next;

    public BlockingStreamIterator(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
        this.observer = new Observer<QueryOk>() {

            @Override
            public void onNext(final QueryOk value) {
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

            this.next = (QueryOk) v;

            return this.next != EOF;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return reject("Interrupted", e);
        }
    }

    @Override
    public Stream<Record> next() {
        if (this.next == null) {
            return reject("Null `next` element");
        }

        if (this.next == EOF) {
            return reject("Reaches the end of the iterator");
        }

        return this.next.mapToRecord();
    }

    public Observer<QueryOk> getObserver() {
        return this.observer;
    }

    private static <T> T reject(final String msg) {
        throw new IteratorException(msg);
    }

    private static <T> T reject(final String msg, final Throwable cause) {
        throw new IteratorException(msg, cause);
    }
}
