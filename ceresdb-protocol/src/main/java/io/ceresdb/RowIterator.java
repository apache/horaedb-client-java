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

import io.ceresdb.models.Row;

/**
 *
 * @author xvyang.xy
 */
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
