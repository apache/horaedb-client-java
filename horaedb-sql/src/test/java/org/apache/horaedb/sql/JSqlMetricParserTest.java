/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.sql;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import io.ceresdb.common.parser.SqlParser;
import io.ceresdb.common.parser.SqlParserFactory;
import io.ceresdb.common.parser.SqlParserFactoryProvider;
import io.ceresdb.common.util.ServiceLoader;

public class JSqlMetricParserTest {

    @Test
    public void loadFromSPITest() {
        final SqlParserFactory factory = ServiceLoader.load(SqlParserFactory.class) //
                .firstOrDefault(() -> SqlParserFactory.DEFAULT);
        Assert.assertTrue(factory instanceof JSqlParserFactory);
    }

    @Test
    public void alterTableTest() {
        final SqlParser parser = getParser("ALTER TABLE test_table_1635254941778 ADD COLUMN c20 UINT64");
        Assert.assertEquals(SqlParser.StatementType.Alter, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941778"), parser.tableNames());
    }

    @Test
    public void descTableTest() {
        final SqlParser parser = getParser("DESCRIBE test_table_1635254941778");
        Assert.assertEquals(SqlParser.StatementType.Describe, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941778"), parser.tableNames());
    }

    @Test
    public void extractMetricNamesTest() {
        final SqlParser parser = getParser(
                "select func1(table1.a), func2(table2.b), * from table1 inner join " + "table2 on table1.id=table2.id");
        final List<String> metricNames = parser.tableNames();

        Assert.assertEquals(Arrays.asList("table1", "table2"), metricNames);
    }

    @Test
    public void extractMetricNames2Test() {
        final SqlParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1 inner join "
                                           + "table2, table3 on table1.id=table2.id and table2.id = table3.id");
        final List<String> metricNames = parser.tableNames();

        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void extractMetricNames3Test() {
        final SqlParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1 join "
                                           + "table2, table3 on table1.id=table2.id and table2.id = table3.id");
        final List<String> metricNames = parser.tableNames();

        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void extractMetricNames4Test() {
        final SqlParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1 left join "
                                           + "table2, table3 on table1.id=table2.id and table2.id = table3.id");
        final List<String> metricNames = parser.tableNames();

        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void extractFromCreateTableWithPrimaryKeyTest() {
        final SqlParser parser = getParser("CREATE TABLE with_primary_key(\n" + "    ts TIMESTAMP NOT NULL,\n"
                                           + "    c1 STRING NOT NULL,\n" + "    c2 STRING NULL,\n"
                                           + "    c3 DOUBLE NULL,\n" + "    c4 STRING NULL,\n" + "    c5 STRING NULL,\n"
                                           + "    TIMESTAMP KEY(ts),\n" + "    PRIMARY KEY(c1, ts)\n"
                                           + ") ENGINE=Analytic WITH (ttl='7d', update_mode='APPEND');");
        final List<String> metricNames = parser.tableNames();

        Assert.assertEquals(Collections.singletonList("with_primary_key"), metricNames);

        final List<String> columns = parser.createColumns().stream()
                .map(col -> col.columnType() + "-" + col.columnName() + "-" + col.valueType())
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("Timestamp-ts-TIMESTAMP", "Field-c1-STRING", "Field-c2-STRING",
                "Field-c3-DOUBLE", "Field-c4-STRING", "Field-c5-STRING"), columns);
    }

    @Test
    public void extractFromCreateTableWithPrimaryKeyTagTest() {
        final SqlParser parser = getParser("CREATE TABLE with_primary_key_tag(\n" + "    ts TIMESTAMP NOT NULL,\n"
                                           + "    c1 STRING TAG NOT NULL,\n" + "    c2 STRING TAG NULL,\n"
                                           + "    c3 STRING TAG NULL,\n" + "    c4 DOUBLE NULL,\n"
                                           + "    c5 STRING NULL,\n" + "    c6 STRING NULL,\n"
                                           + "    c7 TIMESTAMP NULL,\n" + "    TIMESTAMP KEY(ts),\n"
                                           + "    PRIMARY KEY(c1, ts)\n" + ") ENGINE=Analytic;");
        final List<String> metricNames = parser.tableNames();

        Assert.assertEquals(Collections.singletonList("with_primary_key_tag"), metricNames);

        final List<String> columns = parser.createColumns().stream()
                .map(col -> col.columnType() + "-" + col.columnName() + "-" + col.valueType())
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("Timestamp-ts-TIMESTAMP", "Tag-c1-STRING", "Tag-c2-STRING", "Tag-c3-STRING",
                "Field-c4-DOUBLE", "Field-c5-STRING", "Field-c6-STRING", "Field-c7-TIMESTAMP"), columns);
    }

    @Test
    public void extractFromCreateTableWithTagTest() {
        final SqlParser parser = getParser(
                "CREATE TABLE with_tag(\n" + "    ts TIMESTAMP NOT NULL,\n" + "    c1 STRING TAG NOT NULL,\n"
                                           + "    c2 STRING TAG NULL,\n" + "    c3 STRING TAG NULL,\n"
                                           + "    c4 DOUBLE NULL,\n" + "    c5 STRING NULL,\n" + "    c6 STRING NULL,\n"
                                           + "    TIMESTAMP KEY(ts)\n" + ") ENGINE=Analytic;");
        final List<String> metricNames = parser.tableNames();

        Assert.assertEquals(Collections.singletonList("with_tag"), metricNames);

        final List<String> columns = parser.createColumns().stream()
                .map(col -> col.columnType() + "-" + col.columnName() + "-" + col.valueType())
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("Timestamp-ts-TIMESTAMP", "Tag-c1-STRING", "Tag-c2-STRING", "Tag-c3-STRING",
                "Field-c4-DOUBLE", "Field-c5-STRING", "Field-c6-STRING"), columns);
    }

    @Test
    public void dropTableTest() {
        final SqlParser parser = getParser("DROP TABLE xxx_table");
        Assert.assertEquals(SqlParser.StatementType.Drop, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.tableNames());
    }

    @Test
    public void showCreateTableTest() {
        // https://github.com/JSQLParser/JSqlParser/issues/883
        final SqlParser parser = getParser("SHOW xxx_table");
        Assert.assertEquals(SqlParser.StatementType.Show, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.tableNames());
    }

    @Test
    public void insertTest() {
        final SqlParser parser = getParser("INSERT INTO insert_select_test_table (c1, c2, c3) VALUES (1, 'a', 1)");
        Assert.assertEquals(SqlParser.StatementType.Insert, parser.statementType());
        Assert.assertEquals(Collections.singletonList("insert_select_test_table"), parser.tableNames());
    }

    private static SqlParser getParser(final String sql) {
        return SqlParserFactoryProvider.getSqlParserFactory().getParser(sql);
    }
}
