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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.ceresdb.common.util.Requires;
import io.ceresdb.common.util.Strings;

/**
 * The query request condition.
 *
 * @author jiachun.fjc
 */
public class QueryRequest {

    private List<String> metrics = Collections.emptyList();
    private String       ql;

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public String getQl() {
        return ql;
    }

    @Override
    public String toString() {
        return "QueryRequest{" + //
               "metrics=" + metrics + //
               ", ql='" + ql + '\'' + //
               '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static QueryRequest check(final QueryRequest qr) {
        Requires.requireTrue(Strings.isNotBlank(qr.ql), "Empty.ql");
        return qr;
    }

    public static class Builder {
        private final List<String> metrics = new ArrayList<>();
        private String             ql;

        /**
         * Client does not parse the QL, so please fill in which metrics you queried.
         *
         * @param metrics the metrics queried.
         * @return this builder
         */
        public Builder forMetrics(final String... metrics) {
            this.metrics.addAll(Arrays.asList(metrics));
            return this;
        }

        /**
         * Query language to.
         *
         * @param ql the query language
         * @return this builder
         */
        public Builder ql(final String ql) {
            this.ql = ql;
            return this;
        }

        /**
         * Query language to, using the specified format string and arguments.
         *
         * @param fmtQl format ql string
         * @param args  arguments referenced by the format specifiers in the format
         *              QL string.  If there are more arguments than format specifiers,
         *              the extra arguments are ignored.  The number of arguments is
         *               variable and may be zero.
         * @return this builder
         */
        public Builder ql(final String fmtQl, final Object... args) {
            this.ql = String.format(fmtQl, args);
            return this;
        }

        public QueryRequest build() {
            final QueryRequest qr = new QueryRequest();
            qr.metrics = this.metrics;
            qr.ql = this.ql;
            return qr;
        }
    }
}
