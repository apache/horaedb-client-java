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

import com.ceresdb.common.util.Requires;

/**
 *
 * @author jiachun.fjc
 */
public final class Schema {

    public enum Type {
        Avro(String.class), //
        Json(Void.class) // Json do not need schema
        ;

        private final Class<?> javaType;

        Type(Class<?> javaType) {
            this.javaType = javaType;
        }

        public Class<?> getJavaType() {
            return javaType;
        }
    }

    private final Type   type;
    private final Object content;

    private Schema(Type type, Object content) {
        this.type = type;
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public <T> T getContent() {
        switch (this.type) {
            case Avro:
            case Json:
                return (T) this.type.getJavaType().cast(this.content);
            default:
                throw new IllegalArgumentException("invalid type: " + this.type);
        }
    }

    @Override
    public String toString() {
        return "Schema{" + //
               "type=" + type + //
               ", content=" + content + //
               '}';
    }

    public static Schema schema(final Schema.Type type, final Object content) {
        Requires.requireNonNull(type, "Null.type");
        type.getJavaType().cast(content);
        return new Schema(type, content);
    }
}
