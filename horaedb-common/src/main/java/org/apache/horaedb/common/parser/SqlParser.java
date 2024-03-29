/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.parser;

import java.util.Collections;
import java.util.List;

/**
 * A SQL parser that can extract table names from the given SQL.
 *
 */
public interface SqlParser {

    SqlParser DEFAULT = new NoopSqlParser();

    StatementType statementType();

    /**
     * Extract metric(table) names from the given QL.
     *
     * @return metric names
     */
    List<String> tableNames();

    /**
     * Extract the column names and types from the given create table QL.
     *
     * @return columns
     */
    List<Column> createColumns();

    enum StatementType {
        Unknown, Create, Select, Alter, Describe, Show, Drop, Insert, Exists
    }

    interface Column {

        enum ColumnType {
            Unknown, Timestamp, //
            Tag, //
            Field,
        }

        String metricName();

        String columnName();

        default ColumnType columnType() {
            return ColumnType.Unknown;
        }

        default String valueType() {
            return "Unknown";
        }
    }

    class NoopSqlParser implements SqlParser {

        @Override
        public StatementType statementType() {
            return StatementType.Unknown;
        }

        @Override
        public List<String> tableNames() {
            return Collections.emptyList();
        }

        @Override
        public List<Column> createColumns() {
            return Collections.emptyList();
        }
    }
}
