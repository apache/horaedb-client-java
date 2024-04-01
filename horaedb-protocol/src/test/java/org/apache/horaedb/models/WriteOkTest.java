/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.models;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.horaedb.models.WriteOk;
import org.junit.Assert;
import org.junit.Test;

public class WriteOkTest {

    @Test
    public void combineTest() {
        final Set<String> tables = new HashSet<>();
        tables.add("test1");
        final WriteOk writeOk = WriteOk.ok(200, 2, tables);
        writeOk.combine(WriteOk.ok(100, 0, new HashSet<>(Arrays.asList("test2", "test3"))));

        Assert.assertEquals(300, writeOk.getSuccess());
        Assert.assertEquals(2, writeOk.getFailed());
        Assert.assertEquals(new HashSet<>(Arrays.asList("test1", "test2", "test3")), writeOk.getTables());
    }

    @Test
    public void combineWithNullMetrics() {
        final WriteOk writeOk = WriteOk.ok(200, 2, null);
        writeOk.combine(WriteOk.ok(100, 0, new HashSet<>(Arrays.asList("test2", "test3"))));

        Assert.assertEquals(300, writeOk.getSuccess());
        Assert.assertEquals(2, writeOk.getFailed());
        Assert.assertEquals(new HashSet<>(Arrays.asList("test2", "test3")), writeOk.getTables());

        writeOk.combine(WriteOk.ok(100, 0, null));

        Assert.assertEquals(400, writeOk.getSuccess());
        Assert.assertEquals(2, writeOk.getFailed());
        Assert.assertEquals(new HashSet<>(Arrays.asList("test2", "test3")), writeOk.getTables());
    }
}
