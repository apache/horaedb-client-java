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
    public CeresDBxSqlStatement handleStatement(final String sql, //
                                                final StatementType stmtType, //
                                                final String database, //
                                                final List<String> tables, //
                                                final List<Integer> parameters, //
                                                final Map<String, Integer> positions) {
        return null;
    }
}
