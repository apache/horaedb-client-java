/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.signal;

/**
 * Signal types.
 *
 */
public enum Signal {
    SIG_USR2("USR2");

    private final String signalName;

    Signal(String signalName) {
        this.signalName = signalName;
    }

    public String signalName() {
        return signalName;
    }
}
