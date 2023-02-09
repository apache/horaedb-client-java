/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

/**
 * An object pool.
 *
 */
public interface ObjectPool<T> {

    /**
     * Get an object from the pool.
     */
    T getObject();

    /**
     * Return the object to the pool.  The caller should not use the object beyond this point.
     */
    void returnObject(final T returned);

    /**
     * Defines a resource, and the way to create and destroy instances of it.
     */
    interface Resource<T> {

        /**
         * Create a new instance of the resource.
         */
        T create();

        /**
         * Destroy the given instance.
         */
        void close(final T instance);
    }
}
