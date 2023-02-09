/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
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