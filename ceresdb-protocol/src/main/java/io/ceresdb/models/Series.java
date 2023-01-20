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

import java.util.Objects;
import java.util.SortedMap;


/**
 * Series in a metric.
 *
 * @author jiachun.fjc
 */
public class Series {

    private String                      table;
    private SortedMap<String, TagValue> tags;

    public String getTable() {
        return table;
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
        return Objects.equals(table, series.table) && Objects.equals(tags, series.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, tags);
    }

    @Override
    public String toString() {
        return "Series{" + //
               "table='" + table + '\'' + //
               ", tags=" + tags + //
               '}';
    }
}
