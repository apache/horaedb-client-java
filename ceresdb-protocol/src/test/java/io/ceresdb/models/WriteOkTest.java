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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
