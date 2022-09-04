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
package io.ceresdb.rpc;

import java.util.Objects;

/**
 * Description of a remote method.
 *
 * @author jiachun.fjc
 */
public class MethodDescriptor {
    private final String     name;
    private final MethodType type;
    private final double     limitPercent;

    public static MethodDescriptor of(final String name, final MethodType type) {
        return new MethodDescriptor(name, type, -1d);
    }

    public static MethodDescriptor of(final String name, final MethodType type, final double limitPercent) {
        return new MethodDescriptor(name, type, limitPercent);
    }

    public MethodDescriptor(String name, MethodType type, double limitPercent) {
        this.name = name;
        this.type = type;
        this.limitPercent = limitPercent;
    }

    public String getName() {
        return name;
    }

    public MethodType getType() {
        return type;
    }

    public double getLimitPercent() {
        return limitPercent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodDescriptor that = (MethodDescriptor) o;
        return Objects.equals(name, that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "MethodDescriptor{" + //
               "name='" + name + '\'' + //
               ", type=" + type + //
               ", limitPercent=" + limitPercent + //
               '}';
    }

    public enum MethodType {
        /**
         * One request message followed by one response message.
         */
        UNARY,

        /**
         * Zero or more request messages followed by one response message.
         */
        CLIENT_STREAMING,

        /**
         * One request message followed by zero or more response messages.
         */
        SERVER_STREAMING,
    }
}
