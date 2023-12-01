/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

import java.util.Objects;
import java.util.Optional;

import io.ceresdb.common.util.Requires;

public class Value {

    public enum DataType {
        String(String.class), //
        Boolean(Boolean.class), //
        Double(Double.class), //
        Float(Float.class), //
        Int64(Long.class), //
        Int32(Integer.class), //
        Int16(Integer.class), //
        Int8(Integer.class), //
        UInt64(Long.class), //
        UInt32(Integer.class), //
        UInt16(Integer.class), //
        UInt8(Integer.class), //
        Timestamp(Long.class), //
        Varbinary(byte[].class); //

        private final Class<?> javaType;

        DataType(Class<?> javaType) {
            this.javaType = javaType;
        }

        public Class<?> getJavaType() {
            return javaType;
        }
    }

    enum NullValue {
        String(DataType.String), //
        Boolean(DataType.Boolean), //
        Int64(DataType.Int64), //
        Double(DataType.Double), //
        Float(DataType.Float), //
        Int32(DataType.Int32), //
        Int16(DataType.Int16), //
        Int8(DataType.Int8), //
        UInt64(DataType.UInt64), //
        UInt32(DataType.UInt32), //
        UInt16(DataType.UInt16), //
        UInt8(DataType.UInt8), //
        Timestamp(DataType.Timestamp), //
        Varbinary(DataType.Varbinary); //

        private final Value value;

        NullValue(DataType type) {
            this.value = new Value(type, null);
        }
    }

    private final DataType type;
    private final Object   value;

    public Value(DataType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public DataType getDataType() {
        return type;
    }

    public Object getObject() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Value value1 = (Value) o;
        return type == value1.type && value.equals(value1.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "Value{" + //
               "type=" + type + //
               ",value=" + value + //
               '}';
    }

    public boolean isNull() {
        return this.value == null;
    }

    public String getString() {
        return getCheckedValue(DataType.String);
    }

    public Optional<String> getStringOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getString());
    }

    public boolean getBoolean() {
        return getCheckedValue(DataType.Boolean);
    }

    public Optional<Boolean> getBooleanOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getBoolean());
    }

    public double getDouble() {
        return getCheckedValue(DataType.Double);
    }

    public Optional<Double> getDoubleOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getDouble());
    }

    public float getFloat() {
        return getCheckedValue(DataType.Float);
    }

    public Optional<Float> getFloatOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getFloat());
    }

    public long getInt64() {
        return getCheckedValue(DataType.Int64);
    }

    public Optional<Long> getInt64OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getInt64());
    }

    public int getInt32() {
        return getCheckedValue(DataType.Int32);
    }

    public Optional<Integer> getInt32OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getInt32());
    }

    public int getInt16() {
        return getCheckedValue(DataType.Int16);
    }

    public Optional<Integer> getInt16OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getInt16());
    }

    public int getInt8() {
        return getCheckedValue(DataType.Int8);
    }

    public Optional<Integer> getInt8OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getInt8());
    }

    public long getUInt64() {
        return getCheckedValue(DataType.UInt64);
    }

    public Optional<Long> getUInt64OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getUInt64());
    }

    public int getUInt32() {
        return getCheckedValue(DataType.UInt32);
    }

    public Optional<Integer> getUInt32OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getUInt32());
    }

    public int getUInt16() {
        return getCheckedValue(DataType.UInt16);
    }

    public Optional<Integer> getUInt16OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getUInt16());
    }

    public int getUInt8() {
        return getCheckedValue(DataType.UInt8);
    }

    public Optional<Integer> getUInt8OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getUInt8());
    }

    public long getTimestamp() {
        return getCheckedValue(DataType.Timestamp);
    }

    public Optional<Long> getTimestampOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getTimestamp());
    }

    public byte[] getVarbinary() {
        return getCheckedValue(DataType.Varbinary);
    }

    public Optional<byte[]> getVarbinaryOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getVarbinary());
    }

    @SuppressWarnings("unchecked")
    private <T> T getCheckedValue(final DataType type) {
        Requires.requireTrue(this.type == type, "Invalid type %s, expected is %s", this.type, type);
        return (T) type.getJavaType().cast(this.value);
    }

    public static Value withString(final String val) {
        Requires.requireNonNull(val, "Null.val");
        return new Value(DataType.String, val);
    }

    public static Value withStringOrNull(final String val) {
        return val == null ? Value.NullValue.String.value : withString(val);
    }

    public static Value withBoolean(final boolean val) {
        return new Value(DataType.Boolean, val);
    }

    public static Value withBooleanOrNull(final Boolean val) {
        return val == null ? Value.NullValue.Boolean.value : withBoolean(val);
    }

    public static Value withDouble(final double val) {
        return new Value(DataType.Double, val);
    }

    public static Value withDoubleOrNull(final Double val) {
        return val == null ? Value.NullValue.Double.value : withDouble(val);
    }

    public static Value withFloat(final float val) {
        return new Value(DataType.Float, val);
    }

    public static Value withFloatOrNull(final Float val) {
        return val == null ? Value.NullValue.Float.value : withFloat(val);
    }

    public static Value withInt64(final long val) {
        return new Value(DataType.Int64, val);
    }

    public static Value withInt64OrNull(final Long val) {
        return val == null ? Value.NullValue.Int64.value : withInt64(val);
    }

    public static Value withInt32(final int val) {
        return new Value(DataType.Int32, val);
    }

    public static Value withInt32OrNull(final Integer val) {
        return val == null ? Value.NullValue.Int32.value : withInt32(val);
    }

    public static Value withInt16(final int val) {
        return new Value(DataType.Int16, val);
    }

    public static Value withInt16OrNull(final Integer val) {
        return val == null ? Value.NullValue.Int16.value : withInt16(val);
    }

    public static Value withInt8(final int val) {
        return new Value(DataType.Int8, val);
    }

    public static Value withInt8OrNull(final Integer val) {
        return val == null ? Value.NullValue.Int8.value : withInt8(val);
    }

    public static Value withUInt64(final long val) {
        return new Value(DataType.UInt64, val);
    }

    public static Value withUInt64OrNull(final Long val) {
        return val == null ? Value.NullValue.UInt64.value : withUInt64(val);
    }

    public static Value withUInt32(final int val) {
        return new Value(DataType.UInt32, val);
    }

    public static Value withUInt32OrNull(final Integer val) {
        return val == null ? Value.NullValue.UInt32.value : withUInt32(val);
    }

    public static Value withUInt16(final int val) {
        return new Value(DataType.UInt16, val);
    }

    public static Value withUInt16OrNull(final Integer val) {
        return val == null ? Value.NullValue.UInt16.value : withUInt16(val);
    }

    public static Value withUInt8(final int val) {
        return new Value(DataType.UInt8, val);
    }

    public static Value withUInt8OrNull(final Integer val) {
        return val == null ? Value.NullValue.UInt8.value : withUInt8(val);
    }

    public static Value withTimestamp(final long val) {
        return new Value(DataType.Timestamp, val);
    }

    public static Value withTimestampOrNull(final Long val) {
        return val == null ? Value.NullValue.Timestamp.value : withTimestamp(val);
    }

    public static Value withVarbinary(final byte[] val) {
        Requires.requireNonNull(val, "Null.val");
        return new Value(DataType.Varbinary, val);
    }

    public static Value withVarbinaryOrNull(final byte[] val) {
        return val == null ? Value.NullValue.Varbinary.value : withVarbinary(val);
    }

    public static <T extends Value> boolean isNull(final T value) {
        return value == null || value.isNull();
    }
}
