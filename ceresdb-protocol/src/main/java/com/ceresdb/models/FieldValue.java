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
import java.util.Optional;

import com.ceresdb.common.util.Requires;
import com.ceresdb.common.util.UnsignedUtil;

/**
 * Field value can be wrote to database.
 *
 * @author jiachun.fjc
 */
public final class FieldValue extends Value {

    enum NullField {
        Float64(Type.Float64), //
        String(Type.String), //
        Int64(Type.Int64), //
        Float32(Type.Float32), //
        Int32(Type.Int32), //
        Int16(Type.Int16), //
        Int8(Type.Int8), //
        Boolean(Type.Boolean), //
        UInt64(Type.UInt64), //
        UInt32(Type.UInt32), //
        UInt16(Type.UInt16), //
        UInt8(Type.UInt8), //
        Timestamp(Type.Timestamp), //
        Varbinary(Type.Varbinary); //

        private final FieldValue value;

        NullField(Type type) {
            this.value = new FieldValue(type, null);
        }
    }

    private FieldValue(Type type, Object value) {
        super(type, value);
    }

    public double getDouble() {
        return getFloat64();
    }

    public Optional<Double> getDoubleOrNull() {
        return getFloat64OrNull();
    }

    public double getFloat64() {
        return getCheckedValue(Type.Float64);
    }

    public Optional<Double> getFloat64OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getFloat64());
    }

    public String getString() {
        return getCheckedValue(Type.String);
    }

    public Optional<String> getStringOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getString());
    }

    public long getInt64() {
        return getCheckedValue(Type.Int64);
    }

    public Optional<Long> getInt64OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getInt64());
    }

    public float getFloat() {
        return getFloat32();
    }

    public Optional<Float> getFloatOrNull() {
        return getFloat32OrNull();
    }

    public float getFloat32() {
        return getCheckedValue(Type.Float32);
    }

    public Optional<Float> getFloat32OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getFloat32());
    }

    public int getInt() {
        return getInt32();
    }

    public Optional<Integer> getIntOrNull() {
        return getInt32OrNull();
    }

    public int getInt32() {
        return getCheckedValue(Type.Int32);
    }

    public Optional<Integer> getInt32OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getInt32());
    }

    public int getInt16() {
        return getCheckedValue(Type.Int16);
    }

    public Optional<Integer> getInt16OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getInt16());
    }

    public int getInt8() {
        return getCheckedValue(Type.Int8);
    }

    public Optional<Integer> getInt8OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getInt8());
    }

    public boolean getBoolean() {
        return getCheckedValue(Type.Boolean);
    }

    public Optional<Boolean> getBooleanOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getBoolean());
    }

    public long getUInt64() {
        return getCheckedValue(Type.UInt64);
    }

    public Optional<Long> getUInt64OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getUInt64());
    }

    public int getUInt32() {
        return getCheckedValue(Type.UInt32);
    }

    public Optional<Integer> getUInt32OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getUInt32());
    }

    public int getUInt16() {
        return getCheckedValue(Type.UInt16);
    }

    public Optional<Integer> getUInt16OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getUInt16());
    }

    public int getUInt8() {
        return getCheckedValue(Type.UInt8);
    }

    public Optional<Integer> getUInt8OrNull() {
        return isNull() ? Optional.empty() : Optional.of(getUInt8());
    }

    public long getTimestamp() {
        return getCheckedValue(Type.Timestamp);
    }

    public Optional<Long> getTimestampOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getTimestamp());
    }

    public byte[] getVarbinary() {
        return getCheckedValue(Type.Varbinary);
    }

    public Optional<byte[]> getVarbinaryOrNull() {
        return isNull() ? Optional.empty() : Optional.of(getVarbinary());
    }

    public static FieldValue withDouble(final double val) {
        return withFloat64(val);
    }

    public static FieldValue withDoubleOrNull(final Double val) {
        return withFloat64OrNull(val);
    }

    public static FieldValue withFloat64(final double val) {
        return new FieldValue(Type.Float64, val);
    }

    public static FieldValue withFloat64OrNull(final Double val) {
        return val == null ? NullField.Float64.value : withFloat64(val);
    }

    public static FieldValue withString(final String val) {
        Requires.requireNonNull(val, "Null.val");
        return new FieldValue(Type.String, val);
    }

    public static FieldValue withStringOrNull(final String val) {
        return val == null ? NullField.String.value : withString(val);
    }

    public static FieldValue withInt64(final long val) {
        return new FieldValue(Type.Int64, val);
    }

    public static FieldValue withInt64OrNull(final Long val) {
        return val == null ? NullField.Int64.value : withInt64(val);
    }

    public static FieldValue withFloat(final float val) {
        return withFloat32(val);
    }

    public static FieldValue withFloatOrNull(final Float val) {
        return withFloat32OrNull(val);
    }

    public static FieldValue withFloat32(final float val) {
        return new FieldValue(Type.Float32, val);
    }

    public static FieldValue withFloat32OrNull(final Float val) {
        return val == null ? NullField.Float32.value : withFloat32(val);
    }

    public static FieldValue withInt(final int val) {
        return withInt32(val);
    }

    public static FieldValue withIntOrNull(final Integer val) {
        return withInt32OrNull(val);
    }

    public static FieldValue withInt32(final int val) {
        return new FieldValue(Type.Int32, val);
    }

    public static FieldValue withInt32OrNull(final Integer val) {
        return val == null ? NullField.Int32.value : withInt32(val);
    }

    public static FieldValue withInt16(final int val) {
        return new FieldValue(Type.Int16, val);
    }

    public static FieldValue withInt16OrNull(final Integer val) {
        return val == null ? NullField.Int16.value : withInt16(val);
    }

    public static FieldValue withInt8(final int val) {
        return new FieldValue(Type.Int8, val);
    }

    public static FieldValue withInt8OrNull(final Integer val) {
        return val == null ? NullField.Int8.value : withInt8(val);
    }

    public static FieldValue withBoolean(final boolean val) {
        return new FieldValue(Type.Boolean, val);
    }

    public static FieldValue withBooleanOrNull(final Boolean val) {
        return val == null ? NullField.Boolean.value : withBoolean(val);
    }

    public static FieldValue withUInt64(final long val) {
        return new FieldValue(Type.UInt64, val);
    }

    public static FieldValue withUInt64OrNull(final Long val) {
        return val == null ? NullField.UInt64.value : withUInt64(val);
    }

    public static FieldValue withUInt64(final BigInteger val) {
        Requires.requireNonNull(val, "Null.val");
        return new FieldValue(Type.UInt64, UnsignedUtil.getInt64(val));
    }

    public static FieldValue withUInt64OrNull(final BigInteger val) {
        return val == null ? NullField.UInt64.value : withUInt64(val);
    }

    public static FieldValue withUInt32(final int val) {
        return new FieldValue(Type.UInt32, val);
    }

    public static FieldValue withUInt32OrNull(final Integer val) {
        return val == null ? NullField.UInt32.value : withUInt32(val);
    }

    public static FieldValue withUInt16(final int val) {
        return new FieldValue(Type.UInt16, val);
    }

    public static FieldValue withUInt16OrNull(final Integer val) {
        return val == null ? NullField.UInt16.value : withUInt16(val);
    }

    public static FieldValue withUInt8(final int val) {
        return new FieldValue(Type.UInt8, val);
    }

    public static FieldValue withUInt8OrNull(final Integer val) {
        return val == null ? NullField.UInt8.value : withUInt8(val);
    }

    public static FieldValue withTimestamp(final long val) {
        return new FieldValue(Type.Timestamp, val);
    }

    public static FieldValue withTimestampOrNull(final Long val) {
        return val == null ? NullField.Timestamp.value : withTimestamp(val);
    }

    public static FieldValue withVarbinary(final byte[] val) {
        Requires.requireNonNull(val, "Null.val");
        return new FieldValue(Type.Varbinary, val);
    }

    public static FieldValue withVarbinaryOrNull(final byte[] val) {
        return val == null ? NullField.Varbinary.value : withVarbinary(val);
    }
}
