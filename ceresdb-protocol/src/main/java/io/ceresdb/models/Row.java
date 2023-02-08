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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Row {
    private Map<String, Value> values;

    public Row() {
        this.values = new HashMap<>();
    }

    public Value getColumnValue(String column) {
        return this.values.get(column);
    }

    public void setColumnValue(String column, Value value) {
        this.values.put(column, value);
    }

    public int getColumnCount() {
        if (this.values == null) {
            return 0;
        }
        return this.values.size();
    }

    @Override
    public String toString() {
        if (this.values == null || this.values.isEmpty()) {
            return "[Empty Row]";
        }

        return this.values.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue().toString())
                .collect(Collectors.joining("|"));
    }
}