/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.signal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ceresdb.CeresDBClient;
import io.ceresdb.common.SPI;
import io.ceresdb.common.signal.FileSignal;
import io.ceresdb.common.signal.FileSignals;
import io.ceresdb.common.signal.SignalHandler;

/**
 * A signal handler that can clear the route cache.
 *
 */
@SPI(priority = 94)
public class ClearRouteCacheSignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ClearRouteCacheSignalHandler.class);

    @Override
    public void handle(final String signalName) {
        if (FileSignals.ignoreSignal(FileSignal.ClearCache)) {
            return;
        }

        LOG.info("Clear all route cache triggered by signal: {}.", signalName);

        final List<CeresDBClient> instances = CeresDBClient.instances();
        if (instances.isEmpty()) {
            return;
        }

        for (final CeresDBClient ins : instances) {
            final int count = ins.routerClient().clearRouteCache();
            LOG.info("Clearing the client route cache, count={}, client id={}.", count, ins.id());
        }
    }
}
