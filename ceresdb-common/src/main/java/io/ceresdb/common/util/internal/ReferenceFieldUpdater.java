/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util.internal;

public interface ReferenceFieldUpdater<U, W> {

    void set(final U obj, final W newValue);

    W get(final U obj);
}
