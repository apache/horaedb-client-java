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
package com.ceresdb;

import java.util.Collections;
import java.util.List;

/**
 * A metric QL parser that can extract metric names from the given QL.
 *
 * @author jiachun.fjc
 */
public interface MetricParser {

    MetricParser DEFAULT = new NoopMetricParser();

    StatementType statementType();

    /**
     * Extract metric(table) names from the given QL.
     *
     * @return metric names
     */
    List<String> metricNames();

    /**
     * Extract the column names and types from the given create table QL.
     *
     * @return columns
     */
    List<Column> createColumns();

    enum StatementType {
        Unknown, Create, Select, Alter, Describe, Show, Drop, Insert, Exists
    }

    interface Column {

        enum ColumnType {
            Unknown, Timestamp, //
            Tag, //
            Field,
        }

        String metricName();

        String columnName();

        default ColumnType columnType() {
            return ColumnType.Unknown;
        }

        default String valueType() {
            return "Unknown";
        }
    }

    class NoopMetricParser implements MetricParser {

        @Override
        public StatementType statementType() {
            return StatementType.Unknown;
        }

        @Override
        public List<String> metricNames() {
            return Collections.emptyList();
        }

        @Override
        public List<Column> createColumns() {
            return Collections.emptyList();
        }
    }
}
