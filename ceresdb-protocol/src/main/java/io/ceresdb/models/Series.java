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

import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import io.ceresdb.Utils;
import io.ceresdb.common.util.Requires;
import io.ceresdb.common.util.Strings;

/**
 * Series in a metric.
 *
 * @author jiachun.fjc
 */
public class Series {

    private String                      metric;
    private SortedMap<String, TagValue> tags;

    public String getMetric() {
        return metric;
    }

    public SortedMap<String, TagValue> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Series series = (Series) o;
        return Objects.equals(metric, series.metric) && Objects.equals(tags, series.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric, tags);
    }

    @Override
    public String toString() {
        return "Series{" + //
               "metric='" + metric + '\'' + //
               ", tags=" + tags + //
               '}';
    }

    public static Series check(final Series ser) {
        Requires.requireNonNull(ser, "Null.series");
        Requires.requireTrue(Strings.isNotBlank(ser.metric), "Empty.metric");
        Utils.checkKeywords(ser.tags.keySet().iterator());
        return ser;
    }

    public static Builder newBuilder(final String metric) {
        return new Builder(metric);
    }

    public static class Builder {
        private final String                      metric;
        private final SortedMap<String, TagValue> tags;

        public Builder(final String metric) {
            this.metric = metric;
            this.tags = new TreeMap<>();
        }

        /**
         * Puts a tag(key, value), replacing any existing tag of the same key.
         *
         * @param name  tag's name
         * @param value tag's value
         * @return this builder
         */
        public Builder tag(final String name, final String value) {
            this.tags.put(name, TagValue.withString(value));
            return this;
        }

        /**
         * Puts a tag(key, value), replacing any existing tag of the same key.
         *
         * @param name  tag's name
         * @param value tag's value
         * @return this builder
         */
        public Builder tag(final String name, final TagValue value) {
            this.tags.put(name, value);
            return this;
        }

        /**
         * Constructs the series.
         *
         * @return a constructed series
         */
        public Series build() {
            final Series ser = new Series();
            ser.metric = this.metric;
            ser.tags = Collections.unmodifiableSortedMap(this.tags);
            return Series.check(ser);
        }

        /**
         * Build the series and generate a Rows.Builder to continue adding data fields.
         *
         * @return rows.builder
         */
        public Rows.Builder toRowsBuilder() {
            return toRowsBuilder(false);
        }

        /**
         * Build the series and generate a Rows.Builder to continue adding data fields.
         *
         * @param keepFieldOrder whether keep the fields write order
         * @return rows.builder
         */
        public Rows.Builder toRowsBuilder(final boolean keepFieldOrder) {
            final Series ser = build();
            return Rows.newBuilder(ser, keepFieldOrder);
        }
    }
}
