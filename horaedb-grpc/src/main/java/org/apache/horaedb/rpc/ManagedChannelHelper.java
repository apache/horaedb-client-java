/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Managed channel shutdown helper.
 *
 */
public final class ManagedChannelHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedChannelHelper.class);

    /**
     * @see #shutdownAndAwaitTermination(ManagedChannel, long)
     */
    public static boolean shutdownAndAwaitTermination(final ManagedChannel mChannel) {
        return shutdownAndAwaitTermination(mChannel, 1000);
    }

    /**
     * The following method shuts down an {@code ManagedChannel} in two
     * phases, first by calling {@code shutdown} to reject incoming tasks,
     * and then calling {@code shutdownNow}, if necessary, to cancel any
     * lingering tasks.
     */
    public static boolean shutdownAndAwaitTermination(final ManagedChannel mChannel, final long timeoutMillis) {
        if (mChannel == null) {
            return true;
        }
        // disable new tasks from being submitted
        mChannel.shutdown();
        final TimeUnit unit = TimeUnit.MILLISECONDS;
        final long phaseOne = timeoutMillis / 5;
        try {
            // wait a while for existing tasks to terminate
            if (mChannel.awaitTermination(phaseOne, unit)) {
                return true;
            }
            mChannel.shutdownNow();
            // wait a while for tasks to respond to being cancelled
            if (mChannel.awaitTermination(timeoutMillis - phaseOne, unit)) {
                return true;
            }
            LOG.warn("Fail to shutdown managed channel: {}.", mChannel);
        } catch (final InterruptedException e) {
            // (Re-)cancel if current thread also interrupted
            mChannel.shutdownNow();
            // preserve interrupt status
            Thread.currentThread().interrupt();
        }
        return false;
    }
}
