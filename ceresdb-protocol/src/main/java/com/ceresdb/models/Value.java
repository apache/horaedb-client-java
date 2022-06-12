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

import java.util.Objects;

import com.ceresdb.common.util.Requires;

/**
 *
 * @author jiachun.fjc
 */
public class Value {

    public enum Type {
        Float64(Double.class), //
        String(String.class), //
        Int64(Long.class), //
        Float32(Float.class), //
        Int32(Integer.class), //
        Int16(Integer.class), //
        Int8(Integer.class), //
        Boolean(Boolean.class), //
        UInt64(Long.class), //
        UInt32(Integer.class), //
        UInt16(Integer.class), //
        UInt8(Integer.class), //
        Timestamp(Long.class), //
        Varbinary(byte[].class); //

        private final Class<?> javaType;

        Type(Class<?> javaType) {
            this.javaType = javaType;
        }

        public Class<?> getJavaType() {
            return javaType;
        }
    }

    private final Type   type;
    private final Object value;

    protected Value(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
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
               ", value=" + value + //
               '}';
    }

    public boolean isNull() {
        return this.value == null;
    }

    public static <T extends Value> boolean isNull(final T value) {
        return value == null || value.isNull();
    }

    @SuppressWarnings("unchecked")
    protected <T> T getCheckedValue(final Type type) {
        Requires.requireTrue(this.type == type, "Invalid type %s, expected is %s", this.type, type);
        return (T) type.getJavaType().cast(this.value);
    }
}
