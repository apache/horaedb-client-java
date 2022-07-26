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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.ceresdb.Utils;
import com.ceresdb.common.util.Requires;

/**
 * Rows with the same series but different timestamps.
 *
 * @author jiachun.fjc
 */
public class Rows {

    private Series series;

    /**
     * Map<timestamp, Map<name, fieldValue>>
     *
     * Sorted by timestamp, which is useful for data storage.
     */
    private SortedMap<Long, Map<String, FieldValue>> fields;

    public Series getSeries() {
        return series;
    }

    public SortedMap<Long, Map<String, FieldValue>> getFields() {
        return fields;
    }

    public String getMetric() {
        return this.series == null ? null : this.series.getMetric();
    }

    public int getRowCount() {
        return this.fields == null ? 0 : this.fields.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Rows rows = (Rows) o;
        return Objects.equals(series, rows.series) && Objects.equals(fields, rows.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(series, fields);
    }

    @Override
    public String toString() {
        return "Rows{" + //
               "series=" + series + //
               ", fields=" + fields + //
               '}';
    }

    public static Rows check(final Rows rs) {
        Requires.requireNonNull(rs.fields, "Null.fields");
        Requires.requireTrue(!rs.fields.isEmpty(), "Empty.fields");
        Utils.checkKeywords(rs.fields.values().stream().flatMap((map) -> map.keySet().stream()).iterator());
        return rs;
    }

    public static Builder newBuilder(final Series series) {
        return newBuilder(series, false);
    }

    public static Builder newBuilder(final Series series, final boolean keepFieldOrder) {
        return new Builder(series, keepFieldOrder);
    }

    public static class Builder {
        private final Series                                   series;
        private final boolean                                  keepFieldOrder; // whether keep the fields write order
        private final SortedMap<Long, Map<String, FieldValue>> fields;

        public Builder(Series series, boolean keepFieldOrder) {
            this.series = series;
            this.keepFieldOrder = keepFieldOrder;
            this.fields = new TreeMap<>();
        }

        /**
         * Sets a field, replacing any existing field of the same name.
         *
         * @param timestamp  timestamp of this filed
         * @param name       field's name
         * @param fieldValue field's value
         * @return this builder
         */
        public Builder field(final long timestamp, final String name, final FieldValue fieldValue) {
            this.fields //
                    .computeIfAbsent(timestamp, ts -> newFieldMap()) //
                    .put(name, fieldValue);
            return this;
        }

        /**
         * Sets multi fields, replacing any existing field of the same name.
         *
         * @param timestamp timestamp of these fields
         * @param fields    fields map
         * @return this builder
         */
        public Builder fields(final long timestamp, final Map<String, FieldValue> fields) {
            final Map<String, FieldValue> exists = this.fields.get(timestamp);
            if (exists == null) {
                this.fields.put(timestamp, fields);
            } else {
                exists.putAll(fields);
            }
            return this;
        }

        /**
         * Sets multi fields, replacing any existing field of the same name.
         *
         * @param timestamp timestamp of these fields
         * @param input fields input
         * @return this builder
         */
        public Builder fields(final long timestamp, final Consumer<FieldsInput> input) {
            final Map<String, FieldValue> map = this.fields.computeIfAbsent(timestamp, k -> newFieldMap());
            input.accept(map::put);
            return this;
        }

        public interface FieldsInput {
            void put(final String name, final FieldValue fieldValue);
        }

        private Map<String, FieldValue> newFieldMap() {
            return this.keepFieldOrder ? new LinkedHashMap<>() : new HashMap<>();
        }

        /**
         * Constructs the series points.
         *
         * @return Rows
         */
        public Rows build() {
            final Rows rs = new Rows();
            rs.series = this.series;
            rs.fields = Collections.unmodifiableSortedMap(this.fields);
            return Rows.check(rs);
        }
    }
}
