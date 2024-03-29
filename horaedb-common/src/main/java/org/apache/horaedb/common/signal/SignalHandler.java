/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.signal;

/**
 * This is the signal handler interface.
 *
 */
public interface SignalHandler {

    /**
     * The type of signal will be handled.
     *
     * @return signal type
     */
    default Signal signal() {
        return Signal.SIG_USR2;
    }

    /**
     * Handle the given signal.
     *
     * @param signalName signal name
     */
    void handle(final String signalName);
}
