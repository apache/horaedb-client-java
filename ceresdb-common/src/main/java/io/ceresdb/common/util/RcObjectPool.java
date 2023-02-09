/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

/**
 * An shared object pool with ref count.
 *
 */
public class RcObjectPool<T> implements ObjectPool<T> {

    private final Resource<T>         resource;
    private final RcResourceHolder<T> resourceHolder = new RcResourceHolder<>();

    public RcObjectPool(Resource<T> resource) {
        this.resource = resource;
    }

    @Override
    public T getObject() {
        return this.resourceHolder.get(this.resource);
    }

    @Override
    public void returnObject(final T returned) {
        this.resourceHolder.release(this.resource, returned);
    }
}
