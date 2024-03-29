/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.models;

import org.apache.horaedb.models.Point;
import org.apache.horaedb.models.Value;
import org.junit.Assert;
import org.junit.Test;

import io.ceresdb.common.util.Clock;

public class PointTest {
    @Test
    public void PointBuilderTest() {
        Point point = Point.newPointBuilder("test_table").setTimestamp(Clock.defaultClock().getTick())
                .addTag("tag1", "t1").addTag("tag2", "t2").addField("f1", Value.withUInt8(123)).build();

        Assert.assertEquals("t1", point.getTags().get("tag1").getString());
        Assert.assertEquals("t2", point.getTags().get("tag2").getString());
        Assert.assertEquals(123, point.getFields().get("f1").getUInt8());
    }

    @Test(expected = IllegalArgumentException.class)
    public void keywordInTagsTest() {
        Point.newPointBuilder("test_table").setTimestamp(Clock.defaultClock().getTick())
                .addTag("timestamp", Value.withString("ts")) //
                .addTag("tag2", Value.withString("v")) //
                .addField("test", Value.withFloat(0.1f)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void keywordInFieldsTest() {
        Point.newPointBuilder("test_table").setTimestamp(Clock.defaultClock().getTick())
                .addTag("tag1", Value.withString("t1")) //
                .addTag("tag2", Value.withString("t2")) //
                .addField("tsid", Value.withFloat(0.1f)).build();
    }
}
