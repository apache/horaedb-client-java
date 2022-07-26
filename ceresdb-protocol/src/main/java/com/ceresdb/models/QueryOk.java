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
package com.ceresdb.models;

import java.util.function.Function;
import java.util.stream.Stream;

import com.ceresdb.ArrayMapper;
import com.ceresdb.RecordMapper;
import com.ceresdb.common.Streamable;
import com.ceresdb.common.util.Requires;

/**
 * Contains the success value of query.
 *
 * @author jiachun.fjc
 */
public class QueryOk implements Streamable<byte[]> {

    private String         ql;
    private Schema         schema;
    private int            rowCount;
    private Stream<byte[]> rows;

    public String getQl() {
        return ql;
    }

    public Schema getSchema() {
        return schema;
    }

    public int getRowCount() {
        return rowCount;
    }

    public <R> Stream<R> map(final Function<byte[], ? extends R> mapper) {
        if (this.rowCount == 0) {
            return Stream.empty();
        }
        return this.rows.map(mapper);
    }

    public Stream<Record> mapToRecord() {
        if (this.rowCount == 0) {
            return Stream.empty();
        }
        ensureAvroType(this.schema);
        return map(RecordMapper.getMapper(this.schema.getContent()));
    }

    public Stream<Object[]> mapToArray() {
        if (this.rowCount == 0) {
            return Stream.empty();
        }
        ensureAvroType(this.schema);
        return map(ArrayMapper.getMapper(this.schema.getContent()));
    }

    public Result<QueryOk, Err> mapToResult() {
        return Result.ok(this);
    }

    @Override
    public Stream<byte[]> stream() {
        return this.rows;
    }

    @Override
    public String toString() {
        return "QueryOk{" + //
               "ql='" + ql + '\'' + //
               ", schema='" + schema + '\'' + //
               ", rowCount=" + rowCount + //
               '}';
    }

    public static QueryOk emptyOk() {
        return ok("", null, 0, Stream.empty());
    }

    public static QueryOk ok(final String ql, final Schema schema, final int rowCount, final Stream<byte[]> rows) {
        final QueryOk ok = new QueryOk();
        ok.ql = ql;
        ok.schema = schema;
        ok.rowCount = rowCount;
        ok.rows = rows;
        return ok;
    }

    private static void ensureAvroType(final Schema schema) {
        Requires.requireNonNull(schema, "NUll.schema");
        Requires.requireTrue(schema.getType() == Schema.Type.Avro, "Invalid schema type %s, [Avro] type is required",
                schema);
    }
}
