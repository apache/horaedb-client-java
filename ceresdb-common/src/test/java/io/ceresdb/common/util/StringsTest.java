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

import org.junit.Assert;
import org.junit.Test;

public class StringsTest {

    @Test
    public void nullToEmptyTest() {
        Assert.assertEquals("", Strings.nullToEmpty(null));
    }

    @Test
    public void emptyToNullTest() {
        Assert.assertNull(Strings.emptyToNull(""));
        Assert.assertNotNull(Strings.emptyToNull(" "));
    }

    @Test
    public void isNullOrEmptyTest() {
        Assert.assertTrue(Strings.isNullOrEmpty(""));
    }

    @Test
    public void isBlankTest() {
        Assert.assertTrue(Strings.isBlank(null));
        Assert.assertTrue(Strings.isBlank(""));
        Assert.assertTrue(Strings.isBlank(" "));

        Assert.assertFalse(Strings.isBlank("bob"));
        Assert.assertFalse(Strings.isBlank("  bob  "));
    }

    @Test
    public void isNotBlankTest() {
        Assert.assertFalse(Strings.isNotBlank(null));
        Assert.assertFalse(Strings.isNotBlank(""));
        Assert.assertFalse(Strings.isNotBlank(" "));

        Assert.assertTrue(Strings.isNotBlank("bob"));
        Assert.assertTrue(Strings.isNotBlank("  bob  "));
    }

    @Test
    public void splitTest() {
        Assert.assertNull(Strings.split(null, '*'));
        Assert.assertArrayEquals(new String[0], Strings.split("", '*'));
        Assert.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a.b.c", '.'));
        Assert.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a..b.c", '.'));
        Assert.assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", '.'));
        Assert.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a b c", ' '));
    }

    @Test
    public void splitPreserveAllTokensTest() {
        Assert.assertNull(Strings.split(null, '*', true));
        Assert.assertArrayEquals(new String[0], Strings.split("", '*', true));
        Assert.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a.b.c", '.', true));
        Assert.assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.split("a..b.c", '.', true));
        Assert.assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", '.', true));
        Assert.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a b c", ' ', true));
        Assert.assertArrayEquals(new String[] { "a", "b", "c", "" }, Strings.split("a b c ", ' ', true));
        Assert.assertArrayEquals(new String[] { "a", "b", "c", "", "" }, Strings.split("a b c  ", ' ', true));
        Assert.assertArrayEquals(new String[] { "", "a", "b", "c" }, Strings.split(" a b c", ' ', true));
        Assert.assertArrayEquals(new String[] { "", "", "a", "b", "c" }, Strings.split("  a b c", ' ', true));
        Assert.assertArrayEquals(new String[] { "", "a", "b", "c", "" }, Strings.split(" a b c ", ' ', true));
    }
}
