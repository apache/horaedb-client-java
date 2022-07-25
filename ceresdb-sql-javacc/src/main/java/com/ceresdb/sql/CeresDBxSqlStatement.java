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
package com.ceresdb.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class CeresDBxSqlStatement {
    public static final String               DEFAULT_DATABASE   = "system";
    public static final String               DEFAULT_TABLE      = "unknown";
    public static final List<String>         DEFAULT_TABLES     = Collections.singletonList(DEFAULT_TABLE);
    public static final List<Integer>        DEFAULT_PARAMETERS = Collections.emptyList();
    public static final Map<String, Integer> DEFAULT_POSITIONS  = Collections.emptyMap();

    public static final String KEYWORD_EXISTS  = "EXISTS";
    public static final String KEYWORD_REPLACE = "REPLACE";
    public static final String KEYWORD_TOTALS  = "TOTALS";
    public static final String KEYWORD_VALUES  = "VALUES";

    private final String               sql;
    private final StatementType        stmtType;
    private final String               database;
    private final List<String>         tables;
    private final List<Integer>        parameters;
    private final Map<String, Integer> positions;

    public CeresDBxSqlStatement(String sql) {
        this(sql, StatementType.UNKNOWN, null, null, null, null);
    }

    public CeresDBxSqlStatement(String sql, StatementType stmtType) {
        this(sql, stmtType, null, null, null, null);
    }

    public CeresDBxSqlStatement(String sql, //
                                StatementType stmtType, //
                                String database, //
                                List<String> tables, //
                                List<Integer> parameters, //
                                Map<String, Integer> positions) {
        this.sql = sql;
        this.stmtType = stmtType;

        this.database = database;
        this.tables = tables == null || tables.isEmpty() ? DEFAULT_TABLES :
                Collections.unmodifiableList(new ArrayList<>(tables));

        if (parameters != null && parameters.size() > 0) {
            this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
        } else {
            this.parameters = DEFAULT_PARAMETERS;
        }

        if (positions != null && positions.size() > 0) {
            final Map<String, Integer> p = new HashMap<>();
            for (final Entry<String, Integer> e : positions.entrySet()) {
                final String keyword = e.getKey();
                final Integer position = e.getValue();

                if (keyword != null && position != null) {
                    p.put(keyword.toUpperCase(Locale.ROOT), position);
                }
            }
            this.positions = Collections.unmodifiableMap(p);
        } else {
            this.positions = DEFAULT_POSITIONS;
        }
    }

    public String getSQL() {
        return this.sql;
    }

    public boolean isRecognized() {
        return stmtType != StatementType.UNKNOWN;
    }

    public boolean isDDL() {
        return this.stmtType.getLanguageType() == LanguageType.DDL;
    }

    public boolean isDML() {
        return this.stmtType.getLanguageType() == LanguageType.DML;
    }

    public boolean isQuery() {
        return this.stmtType.getOperationType() == OperationType.READ;
    }

    public boolean isMutation() {
        return this.stmtType.getOperationType() == OperationType.WRITE;
    }

    public boolean isIdemponent() {
        boolean result = this.stmtType.isIdempotent();

        if (!result) { // try harder
            switch (this.stmtType) {
                case CREATE:
                case DROP:
                    result = this.positions.containsKey(KEYWORD_EXISTS) || this.positions.containsKey(KEYWORD_REPLACE);
                    break;
                default:
                    break;
            }
        }

        return result;
    }

    public LanguageType getLanguageType() {
        return this.stmtType.getLanguageType();
    }

    public OperationType getOperationType() {
        return this.stmtType.getOperationType();
    }

    public StatementType getStatementType() {
        return this.stmtType;
    }

    public String getDatabase() {
        return this.database;
    }

    @SuppressWarnings("PMD")
    public String getDatabaseOrDefault(String database) {
        return this.database == null ? (database == null ? DEFAULT_DATABASE : database) : this.database;
    }

    public List<String> getTables() {
        return this.tables;
    }

    public boolean containsKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        }

        return positions.containsKey(keyword.toUpperCase(Locale.ROOT));
    }

    public boolean hasWithTotals() {
        return this.positions.containsKey(KEYWORD_TOTALS);
    }

    public boolean hasValues() {
        return this.positions.containsKey(KEYWORD_VALUES);
    }

    public List<Integer> getParameters() {
        return this.parameters;
    }

    public int getStartPosition(String keyword) {
        int position = -1;

        if (!this.positions.isEmpty() && keyword != null) {
            Integer p = this.positions.get(keyword.toUpperCase(Locale.ROOT));
            if (p != null) {
                position = p;
            }
        }

        return position;
    }

    public int getEndPosition(String keyword) {
        int position = getStartPosition(keyword);

        return position != -1 && keyword != null ? position + keyword.length() : position;
    }

    public Map<String, Integer> getPositions() {
        return this.positions;
    }

    @Override
    public String toString() {
        return "CeresDBxSqlStatement{" + //
               "sql='" + sql + '\'' + //
               ", stmtType=" + stmtType + //
               ", database='" + database + '\'' + //
               ", tables='" + tables + '\'' + //
               ", parameters=" + parameters + //
               ", positions=" + positions + //
               '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((database == null) ? 0 : database.hashCode());
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((positions == null) ? 0 : positions.hashCode());
        result = prime * result + ((sql == null) ? 0 : sql.hashCode());
        result = prime * result + ((stmtType == null) ? 0 : stmtType.hashCode());
        result = prime * result + ((tables == null) ? 0 : tables.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CeresDBxSqlStatement other = (CeresDBxSqlStatement) obj;
        if (database == null) {
            if (other.database != null) {
                return false;
            }
        } else if (!database.equals(other.database)) {
            return false;
        }
        if (parameters == null) {
            if (other.parameters != null) {
                return false;
            }
        } else if (!parameters.equals(other.parameters)) {
            return false;
        }
        if (positions == null) {
            if (other.positions != null) {
                return false;
            }
        } else if (!positions.equals(other.positions)) {
            return false;
        }
        if (sql == null) {
            if (other.sql != null) {
                return false;
            }
        } else if (!sql.equals(other.sql)) {
            return false;
        }
        if (stmtType != other.stmtType) {
            return false;
        }
        if (tables == null) {
            return other.tables == null;
        } else {
            return tables.equals(other.tables);
        }
    }
}
