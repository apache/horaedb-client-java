/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Row {
    protected String[] fields;
    protected Value[]  values;

    protected Row() {
    }

    public boolean hasColumn(String name) {
        return getColumnIdx(name) > -1;
    }

    public Column getColumn(String name) {
        int columnIdx = getColumnIdx(name);
        if (columnIdx > -1) {
            return Column.of(name, values[columnIdx]);
        }
        return null;
    }

    public List<Column> getColumns() {
        if (fields == null) {
            return Collections.emptyList();
        }
        List<Column> columns = new ArrayList<>(getColumnCount());
        for (int idx = 0; idx < fields.length; idx++) {
            columns.add(Column.of(fields[idx], values[idx]));
        }
        return columns;
    }

    public int getColumnCount() {
        if (fields == null) {
            return 0;
        }
        return fields.length;
    }

    private int getColumnIdx(String name) {
        if (fields == null) {
            return -1;
        }
        for (int idx = 0; idx < fields.length; idx++) {
            if (fields[idx].equals(name)) {
                return idx;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        if (this.fields == null || this.fields.length == 0) {
            return "[Empty Row]";
        }

        return getColumns().stream().map(column -> column.name + ":" + column.value.toString())
                .collect(Collectors.joining("|"));
    }

    public static class Column {
        public Column(String name, Value value) {
            this.name = name;
            this.value = value;
        }

        private String name;
        private Value  value;

        public String getName() {
            return name;
        }

        public Value getValue() {
            return value;
        }

        public static Column of(String name, Value value) {
            return new Column(name, value);
        }
    }

    public static RowBuilder newRowBuilder(int size) {
        return new RowBuilder(size);
    }

    public static class RowBuilder {
        private Row row;

        public RowBuilder(int size) {
            this.row = new Row();
            this.row.values = new Value[size];
        }

        public void setFields(String[] fields) {
            this.row.fields = fields;
        }

        public void setValue(int colIdx, Value value) {
            this.row.values[colIdx] = value;
        }

        public Row build() {
            return this.row;
        }
    }
}
