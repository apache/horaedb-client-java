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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

public class SpinedBufTest {

    @Test
    public void addTest() {
        addAndCheck(10);
        addAndCheck(100);
        addAndCheck(1000);
        addAndCheck(10000);
        addAndCheck(100000);
    }

    @SuppressWarnings("ConstantConditions")
    public void addAndCheck(final int count) {
        final Collection<Integer> buf1 = Spines.newBuf();
        final Collection<Integer> buf2 = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            buf1.add(i);
            buf2.add(i);
        }

        Assert.assertEquals(buf1, buf2);

        buf1.clear();
        Assert.assertTrue(buf1.isEmpty());
    }

    @Test
    public void sizeTest() {
        final Collection<Integer> buf = Spines.newBuf();
        Assert.assertEquals(0, buf.size());
        add(buf, 1);
        Assert.assertEquals(1, buf.size());
        add(buf, 15);
        Assert.assertEquals(16, buf.size());
    }

    private void add(final Collection<Integer> buf, final int count) {
        for (int i = 0; i < count; i++) {
            buf.add(i);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void isEmptyTest() {
        final Collection<Integer> buf = Spines.newBuf();
        Assert.assertTrue(buf.isEmpty());
        add(buf, 1);
        Assert.assertFalse(buf.isEmpty());
        buf.clear();
        Assert.assertTrue(buf.isEmpty());
    }

    @Test
    public void containsTest() {
        final int count = 10;
        final Collection<Integer> buf = Spines.newBuf();
        add(buf, count);
        for (int i = 0; i < count; i++) {
            Assert.assertTrue(buf.contains(i));
        }

        Assert.assertTrue(buf.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void toArrayTest() {
        final int count = 10;
        final Collection<Integer> buf = Spines.newBuf();
        add(buf, count);
        final Object[] array = buf.toArray();
        int i = 0;
        for (final Integer v : buf) {
            Assert.assertEquals(v, array[i++]);
        }

        final Integer[] array2 = buf.toArray(new Integer[0]);
        i = 0;
        for (final Integer v : buf) {
            Assert.assertEquals(v, array2[i++]);
        }
    }
}
