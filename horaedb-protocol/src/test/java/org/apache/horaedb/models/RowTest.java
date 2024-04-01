/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.models;

import java.util.List;

import org.apache.horaedb.models.Row;
import org.apache.horaedb.models.Value;
import org.junit.Assert;
import org.junit.Test;

public class RowTest {
    @Test
    public void RowBuilderTest() {
        Row.RowBuilder builder = new Row.RowBuilder(5);

        String[] fields = new String[] { "timestamp", "tagA", "tagB", "valueA", "valueB" };
        builder.setValue(4, Value.withInt64(123));
        builder.setValue(2, Value.withString("foo"));
        builder.setValue(0, Value.withTimestamp(12345678L));
        builder.setValue(1, Value.withString("bar"));
        builder.setValue(3, Value.withString("haha"));
        builder.setFields(fields);

        Row row = builder.build();

        Assert.assertEquals(row.getColumnCount(), 5);
        Assert.assertTrue(row.hasColumn("tagA"));
        Assert.assertFalse(row.hasColumn("notExist"));
        Assert.assertEquals(row.getColumn("tagA").getValue().getString(), "bar");
        Assert.assertNull(row.getColumn("notExist"));

        List<Row.Column> columns = row.getColumns();
        Assert.assertEquals(columns.size(), 5);
        Assert.assertEquals(columns.get(1).getValue().getString(), "bar");
        Assert.assertEquals(columns.get(4).getValue().getInt64(), 123);
    }
}
