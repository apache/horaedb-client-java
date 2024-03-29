/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.signal;

import java.util.List;

import org.apache.HoraeDBClient;
import org.apache.horaedb.common.SPI;
import org.apache.horaedb.common.signal.FileSignal;
import org.apache.horaedb.common.signal.FileSignals;
import org.apache.horaedb.common.signal.SignalHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        final List<HoraeDBClient> instances = HoraeDBClient.instances();
        if (instances.isEmpty()) {
            return;
        }

        for (final HoraeDBClient ins : instances) {
            final int count = ins.routerClient().clearRouteCache();
            LOG.info("Clearing the client route cache, count={}, client id={}.", count, ins.id());
        }
    }
}
