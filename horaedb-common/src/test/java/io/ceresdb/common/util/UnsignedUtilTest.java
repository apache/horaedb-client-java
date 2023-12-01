/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Test;

public class UnsignedUtilTest {

    @SuppressWarnings("all")
    @Test
    public void u64Test() {
        final long toAdd = 3;
        long longVal = Long.MAX_VALUE;
        final BigInteger maxLongVal = BigInteger.valueOf(longVal);
        longVal += toAdd;
        Assert.assertTrue(longVal < 0);
        final BigInteger u64Val = UnsignedUtil.getUInt64(longVal);
        Assert.assertEquals(maxLongVal.add(BigInteger.valueOf(toAdd)), u64Val);
        final long newLong = UnsignedUtil.getInt64(u64Val);
        Assert.assertEquals(longVal, newLong);
    }

    @Test
    public void zeroTest() {
        Assert.assertEquals(0, UnsignedUtil.getUInt64(0).longValueExact());
    }
}
