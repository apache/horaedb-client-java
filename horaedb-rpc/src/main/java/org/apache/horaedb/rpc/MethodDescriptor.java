/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc;

import java.util.Objects;

/**
 * Description of a remote method.
 *
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
