/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.horaedb.common.Endpoint;
import org.junit.Assert;
import org.junit.Test;

public class ErrTest {

    @Test
    public void combineWriteOkTest() {
        final Set<String> tables = new HashSet<>();
        tables.add("test_table");
        final WriteOk subOK1 = WriteOk.ok(2, 0, tables);
        Err err = Err.writeErr(300, "test_err", Endpoint.of("127.0.0.1", 8081), Collections.emptyList());
        err = err.combine(subOK1);
        Assert.assertEquals(2, err.getSubOk().getSuccess());

        final Set<String> tables2 = new HashSet<>();
        tables2.add("test_table2");
        final WriteOk subOK2 = WriteOk.ok(3, 0, tables2);
        err = err.combine(subOK2);
        Assert.assertEquals(5, err.getSubOk().getSuccess());
        Assert.assertEquals(new HashSet<>(Arrays.asList("test_table", "test_table2")), err.getSubOk().getTables());
    }

    @Test
    public void combineErrTest() {
        final Err err = Err.writeErr(300, "test_err", Endpoint.of("127.0.0.1", 8081), Collections.emptyList());
        err.combine(Err.writeErr(300, "test_err2", Endpoint.of("127.0.0.1", 8081), Collections.emptyList()));

        final List<Err> list = err.stream().collect(Collectors.toList());

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("test_err", list.get(0).getError());
        Assert.assertEquals("test_err2", list.get(1).getError());
    }
}
