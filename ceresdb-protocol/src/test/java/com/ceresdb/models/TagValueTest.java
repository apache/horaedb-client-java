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
package com.ceresdb.models;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jiachun.fjc
 */
public class TagValueTest {

    @Test
    public void nullableStringTest() {
        final TagValue f1 = TagValue.withStringOrNull("xx");
        Assert.assertTrue(f1.getStringOrNull().isPresent());

        final TagValue f2 = TagValue.withStringOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getStringOrNull().isPresent());
    }

    @Test
    public void nullableInt64Test() {
        final TagValue f1 = TagValue.withInt64OrNull(123L);
        Assert.assertTrue(f1.getInt64OrNull().isPresent());

        final TagValue f2 = TagValue.withInt64OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt64OrNull().isPresent());
    }

    @Test
    public void nullableInt32Test() {
        final TagValue f1 = TagValue.withInt32OrNull(123);
        Assert.assertTrue(f1.getInt32OrNull().isPresent());

        final TagValue f2 = TagValue.withInt32OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt32OrNull().isPresent());
    }

    @Test
    public void nullableInt16Test() {
        final TagValue f1 = TagValue.withInt16OrNull(123);
        Assert.assertTrue(f1.getInt16OrNull().isPresent());

        final TagValue f2 = TagValue.withInt16OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt16OrNull().isPresent());
    }

    @Test
    public void nullableInt8Test() {
        final TagValue f1 = TagValue.withInt8OrNull(123);
        Assert.assertTrue(f1.getInt8OrNull().isPresent());

        final TagValue f2 = TagValue.withInt8OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt8OrNull().isPresent());
    }

    @Test
    public void nullableBooleanTest() {
        final TagValue f1 = TagValue.withBooleanOrNull(true);
        Assert.assertTrue(f1.getBooleanOrNull().isPresent());

        final TagValue f2 = TagValue.withBooleanOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getBooleanOrNull().isPresent());
    }

    @Test
    public void nullableUInt64Test() {
        final TagValue f1 = TagValue.withUInt64OrNull(100L);
        Assert.assertTrue(f1.getUInt64OrNull().isPresent());

        final TagValue f2 = TagValue.withUInt64OrNull((Long) null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt64OrNull().isPresent());

        final TagValue f3 = TagValue.withUInt64OrNull((BigInteger) null);
        Assert.assertTrue(f3.isNull());
        Assert.assertFalse(f3.getUInt64OrNull().isPresent());
    }

    @Test
    public void nullableUInt32Test() {
        final TagValue f1 = TagValue.withUInt32OrNull(123);
        Assert.assertTrue(f1.getUInt32OrNull().isPresent());

        final TagValue f2 = TagValue.withUInt32OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt32OrNull().isPresent());
    }

    @Test
    public void nullableUInt16Test() {
        final TagValue f1 = TagValue.withUInt16OrNull(123);
        Assert.assertTrue(f1.getUInt16OrNull().isPresent());

        final TagValue f2 = TagValue.withUInt16OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt16OrNull().isPresent());
    }

    @Test
    public void nullableUInt8Test() {
        final TagValue f1 = TagValue.withUInt8OrNull(123);
        Assert.assertTrue(f1.getUInt8OrNull().isPresent());

        final TagValue f2 = TagValue.withUInt8OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt8OrNull().isPresent());
    }

    @Test
    public void nullableTimestampTest() {
        final TagValue f1 = TagValue.withTimestampOrNull(123L);
        Assert.assertTrue(f1.getTimestampOrNull().isPresent());

        final TagValue f2 = TagValue.withTimestampOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getTimestampOrNull().isPresent());
    }

    @Test
    public void nullableVarbinaryTest() {
        final TagValue f1 = TagValue.withVarbinaryOrNull(new byte[1]);
        Assert.assertTrue(f1.getVarbinaryOrNull().isPresent());

        final TagValue f2 = TagValue.withVarbinaryOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getVarbinaryOrNull().isPresent());
    }
}
