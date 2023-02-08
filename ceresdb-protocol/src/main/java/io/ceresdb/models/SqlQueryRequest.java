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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.ceresdb.common.util.Requires;
import io.ceresdb.common.util.Strings;

/**
 * The query request condition.
 *
 */
public class SqlQueryRequest {

    private List<String> tables = Collections.emptyList();
    private String       sql;

    public SqlQueryRequest() {
    }

    public SqlQueryRequest(String fmtSql, Object... args) {
        this.sql = this.getSql(fmtSql, args);
    }

    public SqlQueryRequest(List<String> tables, String fmtSql, Object... args) {
        this.tables = tables;
        this.sql = this.getSql(fmtSql, args);
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public String toString() {
        return "QueryRequest{" + //
               "tables=" + tables + //
               ", sql='" + sql + '\'' + //
               '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static SqlQueryRequest check(final SqlQueryRequest qr) {
        Requires.requireTrue(qr.tables != null && qr.tables.size() > 0, "Empty.tables");
        Requires.requireTrue(Strings.isNotBlank(qr.sql), "Empty.sql");
        return qr;
    }

    private String getSql(final String fmtSql, final Object... args) {
        return String.format(fmtSql, args);
    }

    public static class Builder {
        private final List<String> tables = new ArrayList<>();
        private String             sql;

        /**
         * Client does not parse the QL, so please fill in which metrics you queried.
         *
         * @param tables the tables queried.
         * @return this builder
         */
        public Builder forTables(final String... tables) {
            this.tables.addAll(Arrays.asList(tables));
            return this;
        }

        /**
         * Query language to.
         *
         * @param sql the query language
         * @return this builder
         */
        public Builder sql(final String sql) {
            this.sql = sql;
            return this;
        }

        /**
         * Query language to, using the specified format string and arguments.
         *
         * @param fmtQl format ql string
         * @param args  arguments referenced by the format specifiers in the format
         *              QL string.  If there are more arguments than format specifiers,
         *              the extra arguments are ignored.  The number of arguments is
         *               variable and may be zero.
         * @return this builder
         */
        public Builder sql(final String fmtQl, final Object... args) {
            this.sql = String.format(fmtQl, args);
            return this;
        }

        public SqlQueryRequest build() {
            final SqlQueryRequest qr = new SqlQueryRequest();
            qr.tables = this.tables;
            qr.sql = this.sql;
            return qr;
        }
    }
}
