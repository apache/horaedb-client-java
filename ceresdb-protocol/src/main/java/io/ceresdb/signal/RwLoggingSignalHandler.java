/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ceresdb.util.Utils;
import io.ceresdb.common.SPI;
import io.ceresdb.common.signal.FileSignal;
import io.ceresdb.common.signal.Signal;
import io.ceresdb.common.signal.SignalHandler;
import io.ceresdb.common.signal.FileSignals;

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
