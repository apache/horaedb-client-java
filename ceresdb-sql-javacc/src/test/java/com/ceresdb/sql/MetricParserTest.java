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
package com.ceresdb.sql;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ceresdb.MetricParser;
import com.ceresdb.MetricParserFactory;
import com.ceresdb.MetricParserFactoryProvider;
import com.ceresdb.common.util.ServiceLoader;

/**
 * @author jiachun.fjc
 */
public class MetricParserTest {

    @Test
    public void loadFromSPITest() {
        final MetricParserFactory factory = ServiceLoader.load(MetricParserFactory.class) //
                .firstOrDefault(() -> MetricParserFactory.DEFAULT);
        Assert.assertTrue(factory instanceof CeresDBxMetricParserFactory);
    }

    @Test
    public void alterTableTest() {
        final MetricParser parser = getParser("ALTER TABLE test_table_1635254941778 ADD COLUMN c20 UINT64");
        Assert.assertEquals(MetricParser.StatementType.Alter, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941778"), parser.metricNames());
    }

    @Test
    public void descTableTest() {
        MetricParser parser = getParser("DESC TABLE test_table_1635254941779");
        Assert.assertEquals(MetricParser.StatementType.Describe, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941779"), parser.metricNames());

        parser = getParser("DESC test_table_1635254941779");
        Assert.assertEquals(MetricParser.StatementType.Describe, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941779"), parser.metricNames());
    }

    @Test
    public void describeTableTest() {
        MetricParser parser = getParser("DESCRIBE TABLE test_table_1635254941779");
        Assert.assertEquals(MetricParser.StatementType.Describe, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941779"), parser.metricNames());

        parser = getParser("DESCRIBE test_table_1635254941779");
        Assert.assertEquals(MetricParser.StatementType.Describe, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table_1635254941779"), parser.metricNames());
    }

    @Test
    public void extractMetricNamesTest() {
        final MetricParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1, table2, table3 "
                                              + "where table1.id=table2.id");
        final List<String> metricNames = parser.metricNames();
        Assert.assertEquals(MetricParser.StatementType.Select, parser.statementType());
        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void extractMetricNames2Test() {
        final MetricParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1 inner join "
                                              + "table2, table3 on table1.id=table2.id and table2.id = table3.id");
        final List<String> metricNames = parser.metricNames();
        Assert.assertEquals(MetricParser.StatementType.Select, parser.statementType());
        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void extractMetricNames3Test() {
        final MetricParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1 join "
                                              + "table2, table3 on table1.id=table2.id and table2.id = table3.id");
        final List<String> metricNames = parser.metricNames();
        Assert.assertEquals(MetricParser.StatementType.Select, parser.statementType());
        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void extractMetricNames4Test() {
        final MetricParser parser = getParser("select func1(table1.a), func2(table2.b), * from table1 left join "
                                              + "table2, table3 on table1.id=table2.id and table2.id = table3.id");
        final List<String> metricNames = parser.metricNames();
        Assert.assertEquals(MetricParser.StatementType.Select, parser.statementType());
        Assert.assertEquals(Arrays.asList("table1", "table2", "table3"), metricNames);
    }

    @Test
    public void createTableTest() {
        final MetricParser parser = getParser("CREATE TABLE with_primary_key(\n" //
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

        Assert.assertEquals(MetricParser.StatementType.Create, parser.statementType());
        Assert.assertEquals(Collections.singletonList("with_primary_key"), parser.metricNames());
    }

    @Test
    public void createTableIfNotExistsTest() {
        final MetricParser parser = getParser(
                "CREATE TABLE IF NOT EXISTS with_primary_key(\n" + "    ts TIMESTAMP NOT NULL,\n" //
                                              + "    c1 STRING NOT NULL,\n" //
                                              + "    c2 STRING NULL,\n" //
                                              + "    c3 DOUBLE NULL,\n" //
                                              + "    c4 STRING NULL,\n" //
                                              + "    c5 STRING NULL,\n" //
                                              + "    TIMESTAMP KEY(ts),\n" //
                                              + "    PRIMARY KEY(c1, ts)\n" + ")" //
                                              + " ENGINE=Analytic WITH (ttl='7d', update_mode='APPEND');");

        Assert.assertEquals(MetricParser.StatementType.Create, parser.statementType());
        Assert.assertEquals(Collections.singletonList("with_primary_key"), parser.metricNames());
    }

    @Test
    public void dropTableTest() {
        final MetricParser parser = getParser("DROP TABLE test_table");
        Assert.assertEquals(MetricParser.StatementType.Drop, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table"), parser.metricNames());
    }

    @Test
    public void dropTableIfExistsTest() {
        final MetricParser parser = getParser("DROP TABLE IF EXISTS test_table");
        Assert.assertEquals(MetricParser.StatementType.Drop, parser.statementType());
        Assert.assertEquals(Collections.singletonList("test_table"), parser.metricNames());
    }

    @Test
    public void showTest() {
        final MetricParser parser = getParser("SHOW xxx_table");
        Assert.assertEquals(MetricParser.StatementType.Show, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.metricNames());
    }

    @Test
    public void showCreateTableTest() {
        final MetricParser parser = getParser("SHOW CREATE TABLE xxx_table");
        Assert.assertEquals(MetricParser.StatementType.Show, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.metricNames());
    }

    @Test
    public void insertTest() {
        final MetricParser parser = getParser("INSERT INTO insert_select_test_table (*) VALUES (1, 'a', 1)");
        Assert.assertEquals(MetricParser.StatementType.Insert, parser.statementType());
        Assert.assertEquals(Collections.singletonList("insert_select_test_table"), parser.metricNames());
    }

    @Test
    public void existsTest() {
        MetricParser parser = getParser("EXISTS TABLE xxx_table");
        Assert.assertEquals(MetricParser.StatementType.Exists, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.metricNames());

        parser = getParser("EXISTS xxx_table");
        Assert.assertEquals(MetricParser.StatementType.Exists, parser.statementType());
        Assert.assertEquals(Collections.singletonList("xxx_table"), parser.metricNames());
    }

    private static MetricParser getParser(final String sql) {
        return MetricParserFactoryProvider.getMetricParserFactory().getParser(sql);
    }
}
