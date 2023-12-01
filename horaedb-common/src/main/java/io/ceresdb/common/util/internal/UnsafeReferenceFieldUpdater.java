/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util.internal;

import java.lang.reflect.Field;

@SuppressWarnings({ "unchecked", "PMD" })
final class UnsafeReferenceFieldUpdater<U, W> implements ReferenceFieldUpdater<U, W> {

    private final long            offset;
    private final sun.misc.Unsafe unsafe;

    UnsafeReferenceFieldUpdater(sun.misc.Unsafe unsafe, Class<? super U> tClass, String fieldName)
            throws NoSuchFieldException {
        final Field field = tClass.getDeclaredField(fieldName);
        if (unsafe == null) {
            throw new NullPointerException("unsafe");
        }
        this.unsafe = unsafe;
        this.offset = unsafe.objectFieldOffset(field);
    }

    @Override
    public void set(final U obj, final W newValue) {
        this.unsafe.putObject(obj, this.offset, newValue);
    }

    @Override
    public W get(final U obj) {
        return (W) this.unsafe.getObject(obj, this.offset);
    }
}
