/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.sql;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.horaedb.common.parser.SqlParser;
import org.apache.horaedb.common.util.Requires;

public class SqlParserUtil {

    private static final String _TIMESTAMP_KEY_UC = "TIMESTAMP KEY";
    private static final String _TIMESTAMP_KEY_LC = _TIMESTAMP_KEY_UC.toLowerCase();
    // This syntax is not supported in CeresDB
    private static final String _UNIQUE_KEY   = "UNIQUE KEY";
    private static final String _TAG          = "TAG";
    private static final String _CREATE_TABLE = "CREATE TABLE";

    public static List<String> extractTableNames(final Statement stmt) {
        final TablesNamesFinder tablesFinder = new TablesNamesFinder();
        return tablesFinder.getTableList(stmt);
    }

    public static String amendSql(final String sql) {
        final String ucSql = sql.trim().toUpperCase();

        // Can not parse `TIMESTAMP KEY` in create table yet.
        if (ucSql.startsWith(_CREATE_TABLE)) {
            Requires.requireTrue(!ucSql.contains(_UNIQUE_KEY), "`unique key` not supported");
            // Case mixing is not supported
            return sql.replace(_TIMESTAMP_KEY_UC, _UNIQUE_KEY) //
                    .replace(_TIMESTAMP_KEY_LC, _UNIQUE_KEY);
        }

        return sql;
    }

    public static List<SqlParser.Column> extractCreateColumns(final CreateTable createTable) {
        final String metricName = createTable.getTable().getName();

        // timestamp
        final String tsColName = createTable.getIndexes() // must not null
                .stream().filter(SqlParserUtil::isTimestampColumn).flatMap(idx -> idx.getColumnsNames().stream())
                .findFirst().orElseThrow(() -> new IllegalArgumentException("There must be a `timestamp` column"));

        final Set<String> tags = createTable.getColumnDefinitions().stream().filter(SqlParserUtil::isTagColumn)
                .map(ColumnDefinition::getColumnName).collect(Collectors.toSet());

        return createTable.getColumnDefinitions().stream().map(col -> new SqlParser.Column() {

            @Override
            public String metricName() {
                return metricName;
            }

            @Override
            public String columnName() {
                return col.getColumnName();
            }

            @Override
            public ColumnType columnType() {
                if (tsColName.equals(columnName())) {
                    return ColumnType.Timestamp;
                }

                if (tags.contains(columnName())) {
                    return ColumnType.Tag;
                }

                return ColumnType.Field;
            }

            @Override
            public String valueType() {
                return col.getColDataType().getDataType();
            }
        }).collect(Collectors.toList());
    }

    private static boolean isTagColumn(final ColumnDefinition col) {
        final List<String> specs = col.getColumnSpecs();

        if (specs == null || specs.isEmpty()) {
            return false;
        }

        for (final String spec : specs) {
            if (_TAG.equalsIgnoreCase(spec)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isTimestampColumn(final Index idx) {
        return _UNIQUE_KEY.equalsIgnoreCase(idx.getType());
    }
}
