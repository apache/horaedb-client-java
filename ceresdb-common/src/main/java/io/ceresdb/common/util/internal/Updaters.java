/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util.internal;

/**
 * Sometime instead of reflection, better performance.
 *
 */
public class Updaters {

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> IntegerFieldUpdater<U> newIntegerFieldUpdater(final Class<? super U> tClass,
                                                                    final String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeIntegerFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionIntegerFieldUpdater<>(tClass, fieldName);
            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> LongFieldUpdater<U> newLongFieldUpdater(final Class<? super U> tClass, final String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeLongFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionLongFieldUpdater<>(tClass, fieldName);
            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U, W> ReferenceFieldUpdater<U, W> newReferenceFieldUpdater(final Class<? super U> tClass,
                                                                              final String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeReferenceFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionReferenceFieldUpdater<>(tClass, fieldName);
            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
