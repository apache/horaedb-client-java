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
