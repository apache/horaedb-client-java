/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
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
