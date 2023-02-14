/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

import java.math.BigInteger;

public final class UnsignedUtil {

    /**
     * Translates the signed int64 to an unsigned int64.
     *
     * @param val signed int64
     * @return unsigned int64
     */
    public static BigInteger getUInt64(final long val) {
        final BigInteger u64 = BigInteger.valueOf(val & Long.MAX_VALUE);
        return val < 0 ? u64.setBit(Long.SIZE - 1) : u64;
    }

    /**
     * Translates the unsigned int64 to a signed int64.
     *
     * @param val unsigned int64
     * @return signed int64
     */
    public static long getInt64(final BigInteger val) {
        return val.longValue();
    }

    private UnsignedUtil() {
    }
}
