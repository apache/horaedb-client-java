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
package io.ceresdb.models;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import io.ceresdb.common.Streamable;

/**
 * Contains the success value of query.
 *
 */
public class SqlQueryOk implements Streamable<Row> {

    private String    sql;
    private int       affectedRows;
    private List<Row> rows;

    public String getSql() {
        return sql;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public int getRowCount() {
        if (rows == null) {
            return 0;
        }
        return rows.size();
    }

    public List<Row> getRowList() {
        if (rows == null) {
            return Collections.EMPTY_LIST;
        }
        return rows;
    }

    public <R> Stream<R> map(final Function<Row, ? extends R> mapper) {
        return this.stream().map(mapper);
    }

    public Result<SqlQueryOk, Err> mapToResult() {
        return Result.ok(this);
    }

    @Override
    public Stream<Row> stream() {
        if (this.getRowCount() == 0) {
            return Stream.empty();
        }
        return this.rows.stream();
    }

    @Override
    public String toString() {
        return "QueryOk{" + //
               "sql='" + sql + '\'' + //
               ", affectedRows=" + affectedRows + //
               ", rows=" + getRowCount() + //
               '}';
    }

    public static SqlQueryOk emptyOk() {
        return ok("", 0, Collections.EMPTY_LIST);
    }

    public static SqlQueryOk ok(final String sql, final int affectedRows, final List<Row> rows) {
        final SqlQueryOk ok = new SqlQueryOk();
        ok.sql = sql;
        ok.affectedRows = affectedRows;
        ok.rows = rows;
        return ok;
    }
}
