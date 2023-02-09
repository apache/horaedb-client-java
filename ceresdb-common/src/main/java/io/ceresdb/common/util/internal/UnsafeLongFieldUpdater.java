/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util.internal;

import java.lang.reflect.Field;

@SuppressWarnings("PMD")
final class UnsafeLongFieldUpdater<U> implements LongFieldUpdater<U> {

    private final long            offset;
    private final sun.misc.Unsafe unsafe;

    UnsafeLongFieldUpdater(sun.misc.Unsafe unsafe, Class<? super U> tClass, String fieldName)
            throws NoSuchFieldException {
        final Field field = tClass.getDeclaredField(fieldName);
        if (unsafe == null) {
            throw new NullPointerException("unsafe");
        }
        this.unsafe = unsafe;
        this.offset = unsafe.objectFieldOffset(field);
    }

    @Override
    public void set(final U obj, final long newValue) {
        this.unsafe.putLong(obj, this.offset, newValue);
    }

    @Override
    public long get(final U obj) {
        return this.unsafe.getLong(obj, this.offset);
    }
}
