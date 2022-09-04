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

import io.ceresdb.common.util.UnsignedUtil;

/**
 * A record implementation that permits field access by integer index.
 *
 * @author jiachun.fjc
 */
public interface IndexedRecord {

    /**
     * Return the value of a field given its position in the schema.
     *
     * @param i the field position in the schema
     * @return the field value
     */
    Object get(final int i);

    /**
     * @see #get(int)
     */
    default <T> T get(final int i, final Class<T> expectType) {
        return expectType.cast(get(i));
    }

    /**
     * Get a boolean value for the given index.
     *
     * @param i the field index
     * @return the field value
     */
    default Boolean getBoolean(final int i) {
        return get(i, Boolean.class);
    }

    /**
     * @see #getInteger(int)
     */
    default Integer getUInt16(final int i) {
        return getInteger(i);
    }

    /**
     * @see #getInteger(int)
     */
    default Integer getUInt8(final int i) {
        return getInteger(i);
    }

    /**
     * @see #getInteger(int)
     */
    default Integer getInt16(final int i) {
        return getInteger(i);
    }

    /**
     * @see #getInteger(int)
     */
    default Integer getInt8(final int i) {
        return getInteger(i);
    }

    /**
     * Get a integer value for the given index.
     *
     * @param i the field index
     * @return the field value
     */
    default Integer getInteger(final int i) {
        return get(i, Integer.class);
    }

    /**
     * @see #getLong(int)
     */
    default Long getTimestamp(final int i) {
        return getLong(i);
    }

    /**
     * Get a unsigned long value for the given index.
     *
     * @param i the field index
     * @return the field value
     */
    default BigInteger getUInt64(final int i) {
        final Long value = getLong(i);
        return value == null ? null : UnsignedUtil.getUInt64(value);
    }

    /**
     * @see #getLong(int)
     */
    default Long getUInt32(final int i) {
        return getLong(i);
    }

    /**
     * @see #getLong(int)
     */
    default Long getInt64(final int i) {
        return getLong(i);
    }

    /**
     * Get a long value for the given index.
     *
     * @param i the field index
     * @return the field value
     */
    default Long getLong(final int i) {
        return get(i, Long.class);
    }

    /**
     * Get a float value for the given index.
     *
     * @param i the field index
     * @return the field value
     */
    default Float getFloat(final int i) {
        return get(i, Float.class);
    }

    /**
     * Get a double value for the given index.
     *
     * @param i the field index
     * @return the field value
     */
    default Double getDouble(final int i) {
        return get(i, Double.class);
    }

    /**
     * Get a string value for the given index.
     *
     * @param i the field index
     * @return the field value
     */
    default String getString(final int i) {
        return get(i, String.class);
    }

    /**
     * Get a bytes value for the given index.
     *
     * @param i the field index
     * @return the field value
     */
    default byte[] getBytes(final int i) {
        return get(i, byte[].class);
    }
}
