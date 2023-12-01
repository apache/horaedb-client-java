/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.sql;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class ParseHandler {

    /**
     * Handle macro like "#include('/tmp/template.sql')".
     *
     * @param name       name of the macro
     * @param parameters parameters
     * @return output of the macro, could be null or empty string
     */
    public String handleMacro(final String name, final List<String> parameters) {
        return null;
    }

    /**
     * Handle parameter.
     *
     * @param database    database
     * @param tables      tables
     * @param columnIndex columnIndex(starts from 1 not 0)
     * @return parameter value
     */
    public String handleParameter(final String database, final List<String> tables, final int columnIndex) {
        return null;
    }

    /**
     * Handle statement.
     *
     * @param sql        sql statement
     * @param stmtType   statement type
     * @param database   database
     * @param tables     tables
     * @param parameters positions of parameters
     * @param positions  keyword positions
     * @return sql statement, or null means no change
     */
    public CeresDBSqlStatement handleStatement(final String sql, //
                                               final StatementType stmtType, //
                                               final String database, //
                                               final List<String> tables, //
                                               final List<Integer> parameters, //
                                               final Map<String, Integer> positions) {
        return null;
    }
}
