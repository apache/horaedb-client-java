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
import java.util.List;
import java.util.Optional;

import com.ceresdb.common.util.UnsignedUtil;

/**
 * A record, fields are accessible by name as well as by index.
 *
 * @author jiachun.fjc
 */
public interface Record extends IndexedRecord {

    /**
     * Return the value of a field given its name.
     *
     * @param field the field name
     * @return the field value
     */
    Object get(final String field);

    /**
     * @see #get(String)
     */
    default <T> T get(final String field, final Class<T> expectType) {
        return expectType.cast(get(field));
    }

    /**
     * Get a boolean value for the given {@code field}.
     *
     * @param field the field name
     * @return the field value
     */
    default Boolean getBoolean(final String field) {
        return get(field, Boolean.class);
    }

    /**
     * Get a unsigned long value for the given {@code field}.
     *
     * @param field the field name
     * @return the field value
     */
    default Integer getUInt16(final String field) {
        return getInteger(field);
    }

    /**
     * @see #getInteger(String)
     */
    default Integer getUInt8(final String field) {
        return getInteger(field);
    }

    /**
     * @see #getInteger(String)
     */
    default Integer getInt16(final String field) {
        return getInteger(field);
    }

    /**
     * @see #getInteger(String)
     */
    default Integer getInt8(final String field) {
        return getInteger(field);
    }

    /**
     * Get a integer value for the given {@code field}.
     *
     * @param field the field name
     * @return the field value
     */
    default Integer getInteger(final String field) {
        return get(field, Integer.class);
    }

    /**
     * @see #getLong(String)
     */
    default Long getTimestamp(final String field) {
        return getLong(field);
    }

    /**
     * @see #getLong(String)
     */
    default BigInteger getUInt64(final String field) {
        final Long value = getLong(field);
        return value == null ? null : UnsignedUtil.getUInt64(value);
    }

    /**
     * @see #getLong(String)
     */
    default Long getUInt32(final String field) {
        return getLong(field);
    }

    /**
     * @see #getLong(String)
     */
    default Long getInt64(final String field) {
        return getLong(field);
    }

    /**
     * Get a long value for the given {@code field}.
     *
     * @param field the field name
     * @return the field value
     */
    default Long getLong(final String field) {
        return get(field, Long.class);
    }

    /**
     * Get a float value for the given {@code field}.
     *
     * @param field the field name
     * @return the field value
     */
    default Float getFloat(final String field) {
        return get(field, Float.class);
    }

    /**
     * Get a double value for the given {@code field}.
     *
     * @param field the field name
     * @return the field value
     */
    default Double getDouble(final String field) {
        return get(field, Double.class);
    }

    /**
     * Get a string value for the given {@code field}.
     *
     * @param field the field name
     * @return the field value
     */
    default String getString(final String field) {
        return get(field, String.class);
    }

    /**
     * Get a bytes value for the given {@code field}.
     *
     * @param field the field name
     * @return the field value
     */
    default byte[] getBytes(final String field) {
        return get(field, byte[].class);
    }

    /**
     * Return true if record has field with name.
     *
     * @param field the field name
     * @return true if record has field with name: key
     */
    boolean hasField(final String field);

    /**
     * Return the field count of this record.
     *
     * @return the field count
     */
    int getFieldCount();

    /**
     * Return all field descriptors in the record.
     *
     * @return all field descriptors
     */
    List<FieldDescriptor> getFieldDescriptors();

    /**
     * Return the specified field descriptor in the record.
     *
     * @param field field name
     * @return the specified field descriptor
     */
    Optional<FieldDescriptor> getFieldDescriptor(final String field);

    class FieldDescriptor {
        private final String          name;
        private final FieldType       type;
        private final List<FieldType> subTypes; // the type is Union

        public FieldDescriptor(String name, FieldType type, List<FieldType> subTypes) {
            this.name = name;
            this.type = type;
            this.subTypes = subTypes;
        }

        public String getName() {
            return name;
        }

        public FieldType getType() {
            return type;
        }

        public List<FieldType> getSubTypes() {
            return subTypes;
        }

        public boolean isTimestamp() {
            if (isTimestamp(this.type.getLogicalType())) {
                return true;
            }

            for (final FieldType fieldType : this.subTypes) {
                if (isTimestamp(fieldType.getLogicalType())) {
                    return true;
                }
            }

            return false;
        }

        private static boolean isTimestamp(final LogicalType logicalType) {
            return logicalType == LogicalType.TimestampMillis || logicalType == LogicalType.TimestampMicros;
        }

        @Override
        public String toString() {
            return "[" + //
                   "name='" + name + '\'' + //
                   ", type='" + type + '\'' + //
                   ", subTypes='" + subTypes + '\'' + //
                   ']';
        }
    }

    final class FieldType {
        private final Type        type;
        private final LogicalType logicalType;

        public static FieldType of(final Type type, final LogicalType logicalType) {
            return new FieldType(type, logicalType);
        }

        private FieldType(Type type, LogicalType logicalType) {
            this.type = type;
            this.logicalType = logicalType;
        }

        public Type getType() {
            return type;
        }

        public LogicalType getLogicalType() {
            return logicalType;
        }

        @Override
        public String toString() {
            return "FieldType{" + //
                   "type=" + type + //
                   ", logicalType=" + logicalType + //
                   '}';
        }
    }

    /**
     * Data types, represents any valid schema.
     */
    enum Type {
        Null, Double, Float, Bytes, String, Long, Int, Boolean, Union, Unknown
    }

    enum LogicalType {
        TimestampMillis, TimestampMicros, Null, Unknown
    }
}
