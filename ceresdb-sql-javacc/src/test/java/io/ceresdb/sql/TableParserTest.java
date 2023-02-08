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
// Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
package io.ceresdb.sql;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.ceresdb.common.parser.SqlParser;
import io.ceresdb.common.parser.SqlParserFactory;
import io.ceresdb.common.parser.SqlParserFactoryProvider;
import io.ceresdb.common.util.ServiceLoader;

public class TableParserTest {

    @Test
    public void loadFromSPITest() {
        final SqlParserFactory factory = ServiceLoader.load(SqlParserFactory.class) //
                .firstOrDefault(() -> SqlParserFactory.DEFAULT);
        Assert.assertTrue(factory instanceof CeresDBParserFactory);
    }

    @Test
    public void alterTableTest() {
        final SqlParser parser = getParser("ALTER TABLE test_table_1635254941778 ADD COLUMN c20 UINT64");
        Assert.assertEquals(SqlParser.StatementType.Alter, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941778"), parser.tableNames());
    }

    @Test
    public void descTableTest() {
        SqlParser parser = getParser("DESC TABLE test_table_1635254941779");
        Assert.assertEquals(SqlParser.StatementType.Describe, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941779"), parser.tableNames());

        parser = getParser("DESC test_table_1635254941779");
        Assert.assertEquals(SqlParser.StatementType.Describe, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941779"), parser.tableNames());
    }

    @Test
    public void describeTableTest() {
        SqlParser parser = getParser("DESCRIBE TABLE test_table_1635254941779");
        Assert.assertEquals(SqlParser.StatementType.Describe, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941779"), parser.tableNames());

        parser = getParser("DESCRIBE test_table_1635254941779");
        Assert.assertEquals(SqlParser.StatementType.Describe, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941779"), parser.tableNames());
    }

    @Test
    public void extractMetricNamesTest() {
        final SqlParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1, table2, table3 "
                                           + "where table1.id=table2.id");
        final List<String> metricNames = parser.tableNames();
        Assert.assertEquals(SqlParser.StatementType.Select, parser.statementType());
        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void extractMetricNames2Test() {
        final SqlParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1 inner join "
                                           + "table2, table3 on table1.id=table2.id and table2.id = table3.id");
        final List<String> metricNames = parser.tableNames();
        Assert.assertEquals(SqlParser.StatementType.Select, parser.statementType());
        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void extractMetricNames3Test() {
        final SqlParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1 join "
                                           + "table2, table3 on table1.id=table2.id and table2.id = table3.id");
        final List<String> metricNames = parser.tableNames();
        Assert.assertEquals(SqlParser.StatementType.Select, parser.statementType());
        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void extractMetricNames4Test() {
        final SqlParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1 left join "
                                           + "table2, table3 on table1.id=table2.id and table2.id = table3.id");
        final List<String> metricNames = parser.tableNames();
        Assert.assertEquals(SqlParser.StatementType.Select, parser.statementType());
        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void createTableTest() {
        final SqlParser parser = getParser("CREATE TABLE with_primary_key(\n" //
                                           + "    ts TIMESTAMP NOT NULL,\n" //
                                           + "    c1 STRING NOT NULL,\n" //
                                           + "    c2 STRING NULL,\n" //
                                           + "    c3 DOUBLE NULL,\n" //
                                           + "    c4 STRING NULL,\n" //
                                           + "    c5 STRING NULL,\n" //
                                           + "    TIMESTAMP KEY(ts),\n" //
                                           + "    PRIMARY KEY(c1, ts)\n" //
                                           + ")" //
                                           + " ENGINE=Analytic WITH (ttl='7d', update_mode='APPEND');");

        Assert.assertEquals(SqlParser.StatementType.Create, parser.statementType());
        Assert.assertEquals(Collections.singletonList("with_primary_key"), parser.tableNames());
    }

    @Test
    public void createTableIfNotExistsTest() {
        final SqlParser parser = getParser(
                "CREATE TABLE IF NOT EXISTS with_primary_key(\n" + "    ts TIMESTAMP NOT NULL,\n" //
                                           + "    c1 STRING NOT NULL,\n" //
                                           + "    c2 STRING NULL,\n" //
                                           + "    c3 DOUBLE NULL,\n" //
                                           + "    c4 STRING NULL,\n" //
                                           + "    c5 STRING NULL,\n" //
                                           + "    TIMESTAMP KEY(ts),\n" //
                                           + "    PRIMARY KEY(c1, ts)\n" + ")" //
                                           + " ENGINE=Analytic WITH (ttl='7d', update_mode='APPEND');");

        Assert.assertEquals(SqlParser.StatementType.Create, parser.statementType());
        Assert.assertEquals(Collections.singletonList("with_primary_key"), parser.tableNames());
    }

    @Test
    public void dropTableTest() {
        final SqlParser parser = getParser("DROP TABLE test_table");
        Assert.assertEquals(SqlParser.StatementType.Drop, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table"), parser.tableNames());
    }

    @Test
    public void dropTableIfExistsTest() {
        final SqlParser parser = getParser("DROP TABLE IF EXISTS test_table");
        Assert.assertEquals(SqlParser.StatementType.Drop, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table"), parser.tableNames());
    }

    @Test
    public void showTest() {
        final SqlParser parser = getParser("SHOW xxx_table");
        Assert.assertEquals(SqlParser.StatementType.Show, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.tableNames());
    }

    @Test
    public void showCreateTableTest() {
        final SqlParser parser = getParser("SHOW CREATE TABLE xxx_table");
        Assert.assertEquals(SqlParser.StatementType.Show, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.tableNames());
    }

    @Test
    public void insertTest() {
        final SqlParser parser = getParser("INSERT INTO insert_select_test_table (*) VALUES (1, 'a', 1)");
        Assert.assertEquals(SqlParser.StatementType.Insert, parser.statementType());
        Assert.assertEquals(Collections.singletonList("insert_select_test_table"), parser.tableNames());
    }

    @Test
    public void existsTest() {
        SqlParser parser = getParser("EXISTS TABLE xxx_table");
        Assert.assertEquals(SqlParser.StatementType.Exists, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.tableNames());

        parser = getParser("EXISTS xxx_table");
        Assert.assertEquals(SqlParser.StatementType.Exists, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.tableNames());
    }

    private static SqlParser getParser(final String sql) {
        return SqlParserFactoryProvider.getSqlParserFactory().getParser(sql);
    }
}
