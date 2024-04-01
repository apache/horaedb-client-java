/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common;

/**
 * Service life cycle mark interface.
 *
 */
public interface Lifecycle<T> {

    /**
     * Initialize the service with options.
     *
     * @param opts options
     * @return true when successes
     */
    boolean init(final T opts);

    /**
     * Dispose the resources for service.
     */
    void shutdownGracefully();

    default void ensureInitialized() {
    }
}
