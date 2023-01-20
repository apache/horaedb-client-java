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

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

/**
 * Rows with the same series but different timestamps.
 * It is only for submitting write efficiency to perform merging
 * And does not expose this data structure to the outside world
 *
 * @author xvyang.xy
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

    public String getTable() {
        return this.series == null ? null : this.series.getTable();
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
}
