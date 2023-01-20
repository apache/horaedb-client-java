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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import io.ceresdb.Utils;
import io.ceresdb.common.util.Requires;

/**
 * A time series data point with multiple fields
 *
 * @author xvyang.xy
 */
public class Point {
    protected String table;
    protected long                     timestamp;
    protected SortedMap<String, Value> tags;
    protected Map<String, Value>       fields;

    protected Point(String table) {
        this.table = table;
        this.tags = new TreeMap<>();
        this.fields = new HashMap<>();
    }

    public String getTable() {
        return table;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public SortedMap<String, Value> getTags() {
        return tags;
    }

    public Map<String, Value> getFields() {
        return fields;
    }

    public static class PointsBuilder {
        private final String            table;
        protected     Collection<Point> points;

        public PointsBuilder(String table) {
            this.table = table;
        }

        public PointBuilder addPoint() {
            return new PointBuilder(this, this.table);
        }

        public Collection<Point> build() {
            this.points.forEach((point) -> check(point));
            return this.points;
        }

        public static void check(final Point point) {
            Requires.requireNonNull(point.fields, "Null.fields");
            Requires.requireTrue(!point.fields.isEmpty(), "Empty.fields");
            Utils.checkKeywords(point.tags.values().stream().iterator());
            Utils.checkKeywords(point.fields.values().stream().iterator());
        }
    }

    private static class PointBuilder {
        private PointsBuilder root;
        private Point point;

        protected PointBuilder(PointsBuilder root, String table) {
            this.root = root;
            this.point = new Point(table);
        }

        public PointBuilder setTimestamp(long timestamp) {
            this.point.timestamp = timestamp;
            return this;
        }

        public PointBuilder addTag(final String tagKey, final Value tagValue) {
            this.point.tags.put(tagKey, tagValue);
            return this;
        }

        public PointBuilder addField(final String fieldKey, final Value fieldValue) {
            this.point.fields.put(fieldKey, fieldValue);
            return this;
        }

        public PointsBuilder build() {
            this.root.points.add(this.point);
            return this.root;
        }
    }
}