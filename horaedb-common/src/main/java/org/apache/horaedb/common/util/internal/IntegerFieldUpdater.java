/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util.internal;

public interface IntegerFieldUpdater<U> {

    void set(final U obj, final int newValue);

    int get(final U obj);
}
