/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.signal;

import org.apache.horaedb.common.SPI;
import org.apache.horaedb.common.signal.FileSignal;
import org.apache.horaedb.common.signal.FileSignals;
import org.apache.horaedb.common.signal.Signal;
import org.apache.horaedb.common.signal.SignalHandler;
import org.apache.horaedb.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A signal handler that can reset RW_LOGGING by {@link Utils#resetRwLogging()}.
 *
 */
@SPI(priority = 95)
public class RwLoggingSignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RwLoggingSignalHandler.class);

    @Override
    public Signal signal() {
        return Signal.SIG_USR2;
    }

    @Override
    public void handle(final String signalName) {
        if (FileSignals.ignoreSignal(FileSignal.RwLogging)) {
            LOG.info("`RW_LOGGING`={}.", Utils.isRwLogging());
            return;
        }

        final boolean oldValue = Utils.resetRwLogging();
        LOG.info("Reset `RW_LOGGING` to {} triggered by signal: {}.", !oldValue, signalName);
    }
}
