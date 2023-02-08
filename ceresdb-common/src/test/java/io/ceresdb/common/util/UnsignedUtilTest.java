/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
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
