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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import io.ceresdb.common.Endpoint;

/**
 * @author jiachun.fjc
 */
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
        Assert.assertEquals(Arrays.asList("test_table", "test_table2"), err.getSubOk().getTables());
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
