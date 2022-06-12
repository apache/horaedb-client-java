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

import com.ceresdb.models.Record;

/**
 *
 * @author jiachun.fjc
 */
public class RecordIterator implements Iterator<Record> {

    private final BlockingStreamIterator streams;
    private Iterator<Record>             current;

    public RecordIterator(BlockingStreamIterator streams) {
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
    public Record next() {
        return this.current.next();
    }
}
