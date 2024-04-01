/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common;

/**
 * Copiable mark interface.
 *
 * @param <T> data type
 */
public interface Copiable<T> {

    /**
     * Copy current object(deep-clone).
     */
    T copy();
}
