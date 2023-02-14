/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

public enum Keyword {
    Timestamp, //
    Tsid;

    public static boolean isKeyword(final String v) {
        for (final Keyword k : values()) {
            if (k.name().equalsIgnoreCase(v)) {
                return true;
            }
        }
        return false;
    }
}
