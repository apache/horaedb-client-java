/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

import java.util.List;

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

    @Test
    public void TablePointsBuilderTest() {
        List<Point> tablePoints = Point.newTablePointsBuilder("test_table").addPoint()
                .setTimestamp(Clock.defaultClock().getTick()).addTag("tag1", "t1").addTag("tag2", "t2")
                .addField("f1", Value.withUInt8(123)).buildAndContinue().addPoint().addTag("tag1", "t12")
                .addTag("tag2", "t23").addField("f1", Value.withUInt8(1234)).buildAndContinue().build();
        Assert.assertEquals(2, tablePoints.size());
        Assert.assertEquals("t1", tablePoints.get(0).getTags().get("tag1").getString());
        Assert.assertEquals("t2", tablePoints.get(0).getTags().get("tag2").getString());
        Assert.assertEquals(123, tablePoints.get(0).getFields().get("f1").getUInt8());
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
