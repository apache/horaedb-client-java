/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.rpc.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.horaedb.common.SPI;
import org.apache.horaedb.common.signal.FileSignal;
import org.apache.horaedb.common.signal.FileSignals;
import org.apache.horaedb.common.signal.SignalHandler;
import io.ceresdb.rpc.interceptors.ClientRequestLimitInterceptor;

/**
 * A signal handler that can reset LIMIT_SWITCH by {@link ClientRequestLimitInterceptor#resetLimitSwitch()}.
 *
 */
@SPI(priority = 89)
public class RpcLimitSignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RpcLimitSignalHandler.class);

    @Override
    public void handle(final String signalName) {
        if (FileSignals.ignoreSignal(FileSignal.RpcLimit)) {
            LOG.info("`LIMIT_SWITCH`={}.", ClientRequestLimitInterceptor.isLimitSwitchOpen());
            return;
        }

        final boolean oldValue = ClientRequestLimitInterceptor.resetLimitSwitch();
        LOG.warn("Reset `LIMIT_SWITCH` to {} triggered by signal: {}.", !oldValue, signalName);
    }
}
