/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
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

    private RequestContext reqCtx;
    private List<String>   tables = Collections.emptyList();
    private String         sql;

    protected SqlQueryRequest() {
    }

    public SqlQueryRequest(String fmtSql, Object... args) {
        this.sql = this.getSql(fmtSql, args);
    }

    public SqlQueryRequest(List<String> tables, String fmtSql, Object... args) {
        this.tables = tables;
        this.sql = this.getSql(fmtSql, args);
    }

    public RequestContext getReqCtx() {
        return reqCtx;
    }

    public void setReqCtx(RequestContext reqCtx) {
        this.reqCtx = reqCtx;
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
