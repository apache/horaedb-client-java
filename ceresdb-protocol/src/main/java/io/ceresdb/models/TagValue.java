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

import java.math.BigInteger;
import java.util.Optional;

import io.ceresdb.common.util.Requires;
import io.ceresdb.common.util.UnsignedUtil;

/**
 * Tag value can be wrote to database.
 *
 * @author jiachun.fjc
 */
public final class TagValue extends Value {

    enum NullTag {
        String(Type.String), //
        Int64(Type.Int64), //
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

        private final TagValue value;

        NullTag(Type type) {
            this.value = new TagValue(type, null);
        }
    }

    private TagValue(Type type, Object value) {
        super(type, value);
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

    public static TagValue withString(final String val) {
        Requires.requireNonNull(val, "Null.val");
        return new TagValue(Type.String, val);
    }

    public static TagValue withStringOrNull(final String val) {
        return val == null ? NullTag.String.value : withString(val);
    }

    public static TagValue withInt64(final long val) {
        return new TagValue(Type.Int64, val);
    }

    public static TagValue withInt64OrNull(final Long val) {
        return val == null ? NullTag.Int64.value : withInt64(val);
    }

    public static TagValue withInt(final int val) {
        return withInt32(val);
    }

    public static TagValue withIntOrNull(final Integer val) {
        return withInt32OrNull(val);
    }

    public static TagValue withInt32(final int val) {
        return new TagValue(Type.Int32, val);
    }

    public static TagValue withInt32OrNull(final Integer val) {
        return val == null ? NullTag.Int32.value : withInt32(val);
    }

    public static TagValue withInt16(final int val) {
        return new TagValue(Type.Int16, val);
    }

    public static TagValue withInt16OrNull(final Integer val) {
        return val == null ? NullTag.Int16.value : withInt16(val);
    }

    public static TagValue withInt8(final int val) {
        return new TagValue(Type.Int8, val);
    }

    public static TagValue withInt8OrNull(final Integer val) {
        return val == null ? NullTag.Int8.value : withInt8(val);
    }

    public static TagValue withBoolean(final boolean val) {
        return new TagValue(Type.Boolean, val);
    }

    public static TagValue withBooleanOrNull(final Boolean val) {
        return val == null ? NullTag.Boolean.value : withBoolean(val);
    }

    public static TagValue withUInt64(final long val) {
        return new TagValue(Type.UInt64, val);
    }

    public static TagValue withUInt64OrNull(final Long val) {
        return val == null ? NullTag.UInt64.value : withUInt64(val);
    }

    public static TagValue withUInt64(final BigInteger val) {
        Requires.requireNonNull(val, "Null.val");
        return new TagValue(Type.UInt64, UnsignedUtil.getInt64(val));
    }

    public static TagValue withUInt64OrNull(final BigInteger val) {
        return val == null ? NullTag.UInt64.value : withUInt64(val);
    }

    public static TagValue withUInt32(final int val) {
        return new TagValue(Type.UInt32, val);
    }

    public static TagValue withUInt32OrNull(final Integer val) {
        return val == null ? NullTag.UInt32.value : withUInt32(val);
    }

    public static TagValue withUInt16(final int val) {
        return new TagValue(Type.UInt16, val);
    }

    public static TagValue withUInt16OrNull(final Integer val) {
        return val == null ? NullTag.UInt16.value : withUInt16(val);
    }

    public static TagValue withUInt8(final int val) {
        return new TagValue(Type.UInt8, val);
    }

    public static TagValue withUInt8OrNull(final Integer val) {
        return val == null ? NullTag.UInt8.value : withUInt8(val);
    }

    public static TagValue withTimestamp(final long val) {
        return new TagValue(Type.Timestamp, val);
    }

    public static TagValue withTimestampOrNull(final Long val) {
        return val == null ? NullTag.Timestamp.value : withTimestamp(val);
    }

    public static TagValue withVarbinary(final byte[] val) {
        Requires.requireNonNull(val, "Null.val");
        return new TagValue(Type.Varbinary, val);
    }

    public static TagValue withVarbinaryOrNull(final byte[] val) {
        return val == null ? NullTag.Varbinary.value : withVarbinary(val);
    }
}
