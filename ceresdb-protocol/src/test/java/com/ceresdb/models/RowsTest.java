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
package com.ceresdb.models;

import java.util.HashMap;

import org.junit.Test;

import com.ceresdb.common.util.Clock;

/**
 *
 * @author jiachun.fjc
 */
public class RowsTest {

    @Test(expected = IllegalArgumentException.class)
    public void keywordInTagsTest() {
        Series.newBuilder("test_metric") //
                .tag("timestamp", "ts") //
                .tag("tag2", "v") //
                .toRowsBuilder() //
                .field(Clock.defaultClock().getTick(), "test", FieldValue.withFloat(0.1f)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void keywordInFieldsTest() {
        Series.newBuilder("test_metric") //
                .tag("tag1", "ts") //
                .tag("tag2", "v") //
                .toRowsBuilder() //
                .field(Clock.defaultClock().getTick(), "tsid", FieldValue.withFloat(0.1f)) //
                .build();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unmodifiableTagsTest() {
        final Series series = Series.newBuilder("test_metric") //
                .tag("tag1", "ts") //
                .tag("tag2", "v") //
                .build();

        series.getTags().put("not_allowed", TagValue.withString("test"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unmodifiableFieldsTest() {
        final Rows rs = Series.newBuilder("test_metric") //
                .tag("tag1", "ts") //
                .tag("tag2", "v") //
                .toRowsBuilder() //
                .field(Clock.defaultClock().getTick(), "test", FieldValue.withFloat(0.1f)) //
                .build();

        rs.getFields().put(Clock.defaultClock().getTick(), new HashMap<>());
    }
}
