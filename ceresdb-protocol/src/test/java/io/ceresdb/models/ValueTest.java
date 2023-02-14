/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

import org.junit.Assert;
import org.junit.Test;

public class ValueTest {

    @Test
    public void nullableStringTest() {
        final Value f1 = Value.withStringOrNull("xx");
        Assert.assertTrue(f1.getStringOrNull().isPresent());

        final Value f2 = Value.withStringOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getStringOrNull().isPresent());
    }

    @Test
    public void nullableBooleanTest() {
        final Value f1 = Value.withBooleanOrNull(true);
        Assert.assertTrue(f1.getBooleanOrNull().isPresent());

        final Value f2 = Value.withBooleanOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getBooleanOrNull().isPresent());
    }

    @Test
    public void nullableDoubleTest() {
        final Value f1 = Value.withDouble(0.1);
        Assert.assertTrue(f1.getDoubleOrNull().isPresent());

        final Value f2 = Value.withDoubleOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getDoubleOrNull().isPresent());
    }

    @Test
    public void nullableFloatTest() {
        final Value f1 = Value.withFloatOrNull(123.01f);
        Assert.assertTrue(f1.getFloatOrNull().isPresent());

        final Value f2 = Value.withFloatOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getFloatOrNull().isPresent());
    }

    @Test
    public void nullableInt64Test() {
        final Value f1 = Value.withInt64OrNull(123L);
        Assert.assertTrue(f1.getInt64OrNull().isPresent());

        final Value f2 = Value.withInt64OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt64OrNull().isPresent());
    }

    @Test
    public void nullableInt32Test() {
        final Value f1 = Value.withInt32OrNull(123);
        Assert.assertTrue(f1.getInt32OrNull().isPresent());

        final Value f2 = Value.withInt32OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt32OrNull().isPresent());
    }

    @Test
    public void nullableInt16Test() {
        final Value f1 = Value.withInt16OrNull(123);
        Assert.assertTrue(f1.getInt16OrNull().isPresent());

        final Value f2 = Value.withInt16OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt16OrNull().isPresent());
    }

    @Test
    public void nullableInt8Test() {
        final Value f1 = Value.withInt8OrNull(123);
        Assert.assertTrue(f1.getInt8OrNull().isPresent());

        final Value f2 = Value.withInt8OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getInt8OrNull().isPresent());
    }

    @Test
    public void nullableUInt64Test() {
        final Value f1 = Value.withUInt64OrNull(100L);
        Assert.assertTrue(f1.getUInt64OrNull().isPresent());

        final Value f2 = Value.withUInt64OrNull((Long) null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt64OrNull().isPresent());
    }

    @Test
    public void nullableUInt32Test() {
        final Value f1 = Value.withUInt32OrNull(123);
        Assert.assertTrue(f1.getUInt32OrNull().isPresent());

        final Value f2 = Value.withUInt32OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt32OrNull().isPresent());
    }

    @Test
    public void nullableUInt16Test() {
        final Value f1 = Value.withUInt16OrNull(123);
        Assert.assertTrue(f1.getUInt16OrNull().isPresent());

        final Value f2 = Value.withUInt16OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt16OrNull().isPresent());
    }

    @Test
    public void nullableUInt8Test() {
        final Value f1 = Value.withUInt8OrNull(123);
        Assert.assertTrue(f1.getUInt8OrNull().isPresent());

        final Value f2 = Value.withUInt8OrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getUInt8OrNull().isPresent());
    }

    @Test
    public void nullableTimestampTest() {
        final Value f1 = Value.withTimestampOrNull(123L);
        Assert.assertTrue(f1.getTimestampOrNull().isPresent());

        final Value f2 = Value.withTimestampOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getTimestampOrNull().isPresent());
    }

    @Test
    public void nullableVarbinaryTest() {
        final Value f1 = Value.withVarbinaryOrNull(new byte[1]);
        Assert.assertTrue(f1.getVarbinaryOrNull().isPresent());

        final Value f2 = Value.withVarbinaryOrNull(null);
        Assert.assertTrue(f2.isNull());
        Assert.assertFalse(f2.getVarbinaryOrNull().isPresent());
    }
}
