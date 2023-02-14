/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import io.ceresdb.util.Utils;
import io.ceresdb.common.util.Requires;

/**
 * A time series data point with multiple fields
 *
 */
public class Point {
    protected String                   table;
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

    public static PointBuilder newPointBuilder(String table) {
        return new PointBuilder(table);
    }

    public static class PointBuilder {
        private Point point;

        protected PointBuilder(String table) {
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

        public PointBuilder addTag(final String tagKey, final String tagValue) {
            this.point.tags.put(tagKey, Value.withStringOrNull(tagValue));
            return this;
        }

        public PointBuilder addField(final String fieldKey, final Value fieldValue) {
            this.point.fields.put(fieldKey, fieldValue);
            return this;
        }

        public Point build() {
            check(this.point);
            return point;
        }

        private static void check(final Point point) throws IllegalArgumentException {
            Requires.requireNonNull(point.fields, "Null.fields");
            Requires.requireTrue(!point.fields.isEmpty(), "Empty.fields");
            Utils.checkKeywords(point.tags.keySet().stream().iterator());
            Utils.checkKeywords(point.fields.keySet().stream().iterator());
        }
    }
}
