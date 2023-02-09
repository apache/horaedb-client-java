/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

import org.junit.Test;

import io.ceresdb.common.util.Clock;

public class PointTest {

    @Test(expected = IllegalArgumentException.class)
    public void keywordInTagsTest() {
        Point.newPointsBuilder("test_table").addPoint().setTimestamp(Clock.defaultClock().getTick())
                .addTag("timestamp", Value.withString("ts")) //
                .addTag("tag2", Value.withString("v")) //
                .addField("test", Value.withFloat(0.1f)).build().build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void keywordInFieldsTest() {
        Point.newPointsBuilder("test_table").addPoint().setTimestamp(Clock.defaultClock().getTick())
                .addTag("tag1", Value.withString("t1")) //
                .addTag("tag2", Value.withString("t2")) //
                .addField("tsid", Value.withFloat(0.1f)).build().build();
    }
}
