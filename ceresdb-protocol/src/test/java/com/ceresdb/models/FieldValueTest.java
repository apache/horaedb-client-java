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
public class FieldValueTest {

    @Test
    public void nullableDoubleTest() {
        final FieldValue f1 = FieldValue.withDoubleOrNull(0.1);
        Assert.assertTrue(f1.getDoubleOrNull().isPresent());

        final FieldValue f2 = FieldValue.withDoubleOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getDoubleOrNull().isPresent());
    }

    @Test
    public void nullableStringTest() {
        final FieldValue f1 = FieldValue.withStringOrNull("xx");
        Assert.assertTrue(f1.getStringOrNull().isPresent());

        final FieldValue f2 = FieldValue.withStringOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getStringOrNull().isPresent());
    }

    @Test
    public void nullableInt64Test() {
        final FieldValue f1 = FieldValue.withInt64OrNull(123L);
        Assert.assertTrue(f1.getInt64OrNull().isPresent());

        final FieldValue f2 = FieldValue.withInt64OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt64OrNull().isPresent());
    }

    @Test
    public void nullableFloat32Test() {
        final FieldValue f1 = FieldValue.withFloat32OrNull(123.01f);
        Assert.assertTrue(f1.getFloat32OrNull().isPresent());

        final FieldValue f2 = FieldValue.withFloat32OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getFloat32OrNull().isPresent());
    }

    @Test
    public void nullableInt32Test() {
        final FieldValue f1 = FieldValue.withInt32OrNull(123);
        Assert.assertTrue(f1.getInt32OrNull().isPresent());

        final FieldValue f2 = FieldValue.withInt32OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt32OrNull().isPresent());
    }

    @Test
    public void nullableInt16Test() {
        final FieldValue f1 = FieldValue.withInt16OrNull(123);
        Assert.assertTrue(f1.getInt16OrNull().isPresent());

        final FieldValue f2 = FieldValue.withInt16OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt16OrNull().isPresent());
    }

    @Test
    public void nullableInt8Test() {
        final FieldValue f1 = FieldValue.withInt8OrNull(123);
        Assert.assertTrue(f1.getInt8OrNull().isPresent());

        final FieldValue f2 = FieldValue.withInt8OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt8OrNull().isPresent());
    }

    @Test
    public void nullableBooleanTest() {
        final FieldValue f1 = FieldValue.withBooleanOrNull(true);
        Assert.assertTrue(f1.getBooleanOrNull().isPresent());

        final FieldValue f2 = FieldValue.withBooleanOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getBooleanOrNull().isPresent());
    }

    @Test
    public void nullableUInt64Test() {
        final FieldValue f1 = FieldValue.withUInt64OrNull(100L);
        Assert.assertTrue(f1.getUInt64OrNull().isPresent());

        final FieldValue f2 = FieldValue.withUInt64OrNull((Long) null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt64OrNull().isPresent());

        final FieldValue f3 = FieldValue.withUInt64OrNull((BigInteger) null);
        Assert.assertTrue(f3.isNull());
        Assert.assertFalse(f3.getUInt64OrNull().isPresent());
    }

    @Test
    public void nullableUInt32Test() {
        final FieldValue f1 = FieldValue.withUInt32OrNull(123);
        Assert.assertTrue(f1.getUInt32OrNull().isPresent());

        final FieldValue f2 = FieldValue.withUInt32OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt32OrNull().isPresent());
    }

    @Test
    public void nullableUInt16Test() {
        final FieldValue f1 = FieldValue.withUInt16OrNull(123);
        Assert.assertTrue(f1.getUInt16OrNull().isPresent());

        final FieldValue f2 = FieldValue.withUInt16OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt16OrNull().isPresent());
    }

    @Test
    public void nullableUInt8Test() {
        final FieldValue f1 = FieldValue.withUInt8OrNull(123);
        Assert.assertTrue(f1.getUInt8OrNull().isPresent());

        final FieldValue f2 = FieldValue.withUInt8OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt8OrNull().isPresent());
    }

    @Test
    public void nullableTimestampTest() {
        final FieldValue f1 = FieldValue.withTimestampOrNull(123L);
        Assert.assertTrue(f1.getTimestampOrNull().isPresent());

        final FieldValue f2 = FieldValue.withTimestampOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getTimestampOrNull().isPresent());
    }

    @Test
    public void nullableVarbinaryTest() {
        final FieldValue f1 = FieldValue.withVarbinaryOrNull(new byte[1]);
        Assert.assertTrue(f1.getVarbinaryOrNull().isPresent());

        final FieldValue f2 = FieldValue.withVarbinaryOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getVarbinaryOrNull().isPresent());
    }
}
