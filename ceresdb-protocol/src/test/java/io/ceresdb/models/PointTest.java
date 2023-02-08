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
