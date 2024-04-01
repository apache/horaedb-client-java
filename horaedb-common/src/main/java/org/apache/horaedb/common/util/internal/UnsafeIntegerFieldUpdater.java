/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util.internal;

import java.lang.reflect.Field;

@SuppressWarnings("PMD")
final class UnsafeIntegerFieldUpdater<U> implements IntegerFieldUpdater<U> {

    private final long            offset;
    private final sun.misc.Unsafe unsafe;

    UnsafeIntegerFieldUpdater(sun.misc.Unsafe unsafe, Class<? super U> tClass, String fieldName)
            throws NoSuchFieldException {
        final Field field = tClass.getDeclaredField(fieldName);
        if (unsafe == null) {
            throw new NullPointerException("unsafe");
        }
        this.unsafe = unsafe;
        this.offset = unsafe.objectFieldOffset(field);
    }

    @Override
    public void set(final U obj, final int newValue) {
        this.unsafe.putInt(obj, this.offset, newValue);
    }

    @Override
    public int get(final U obj) {
        return this.unsafe.getInt(obj, this.offset);
    }
}
