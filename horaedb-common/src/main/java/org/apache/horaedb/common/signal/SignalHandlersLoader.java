/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.horaedb.common.OptKeys;
import org.apache.horaedb.common.util.ServiceLoader;
import org.apache.horaedb.common.util.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tool class for loading and registering all signals.
 *
 * Do not support windows.
 *
 */
public class SignalHandlersLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SignalHandlersLoader.class);

    private static final boolean USE_OS_SIGNAL = SystemPropertyUtil.getBool(OptKeys.USE_OS_SIGNAL, true);

    /**
     * Load and register all signals.
     */
    public static void load() {
        try {
            if (USE_OS_SIGNAL && SignalHelper.supportSignal()) {
                final List<SignalHandler> handlers = ServiceLoader.load(SignalHandler.class) //
                        .sort();

                LOG.info("Loaded signals: {}.", handlers);

                final Map<Signal, List<SignalHandler>> mapTo = new HashMap<>();
                handlers.forEach(h -> mapTo.computeIfAbsent(h.signal(), sig -> new ArrayList<>()) //
                        .add(h));

                mapTo.forEach((sig, hs) -> {
                    final boolean success = SignalHelper.addSignal(sig, hs);
                    LOG.info("Add signal [{}] handler {} {}.", sig, hs, success ? "success" : "failed");
                });
            }
        } catch (final Throwable t) {
            LOG.error("Fail to add signal.", t);
        }
    }
}
