/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache;

import java.util.Iterator;

import org.apache.horaedb.models.Row;

public class RowIterator implements Iterator<Row> {

    private final BlockingStreamIterator streams;
    private Iterator<Row>                current;

    public RowIterator(BlockingStreamIterator streams) {
        this.streams = streams;
    }

    @Override
    public boolean hasNext() {
        if (this.current != null && this.current.hasNext()) {
            return true;
        }
        while (this.streams.hasNext()) {
            this.current = this.streams.next().iterator();
            if (this.current.hasNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Row next() {
        return this.current.next();
    }
}
