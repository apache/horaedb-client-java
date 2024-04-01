/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

public class RefCell<T> {

    private T value;

    public T get() {
        return this.value;
    }

    public void set(final T value) {
        this.value = value;
    }
}
