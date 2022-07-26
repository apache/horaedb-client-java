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

import com.ceresdb.MetricParser;
import com.ceresdb.Utils;
import com.ceresdb.common.util.internal.ThrowUtil;

/**
 * Parse metric QL.
 *
 * @author jiachun.fjc
 */
public class CeresDBxMetricParser implements MetricParser {

    private final String ql;

    private boolean              parsed;
    private StatementType        statementType = StatementType.Unknown;
    private CeresDBxSqlStatement stmt;

    public CeresDBxMetricParser(String ql) {
        this.ql = ql;
    }

    @Override
    public StatementType statementType() {
        parse();

        return this.statementType;
    }

    @Override
    public List<String> metricNames() {
        parse();

        return this.stmt.getTables();
    }

    @Override
    public List<Column> createColumns() {
        parse();

        return Utils.unsupported("`%s` unsupported yet!", "createColumns");
    }

    private void parse() {
        if (this.parsed) {
            return;
        }

        this.parsed = true;

        try {
            final CeresDBxSqlStatement stmt = CeresDBxSqlParser.parse(this.ql)[0];

            switch (stmt.getStatementType()) {
                case SELECT:
                    this.statementType = StatementType.Select;
                    break;
                case CREATE:
                    this.statementType = StatementType.Create;
                    break;
                case ALTER:
                case ALTER_DELETE:
                case ALTER_UPDATE:
                    this.statementType = StatementType.Alter;
                    break;
                case DESCRIBE:
                    this.statementType = StatementType.Describe;
                    break;
                case SHOW:
                    this.statementType = StatementType.Show;
                    break;
                case DROP:
                    this.statementType = StatementType.Drop;
                    break;
                case INSERT:
                    this.statementType = StatementType.Insert;
                    break;
                case EXISTS:
                    this.statementType = StatementType.Exists;
                    break;
                default:
                    this.statementType = StatementType.Unknown;
            }

            this.stmt = stmt;
        } catch (final Exception e) {
            ThrowUtil.throwException(e);
        }
    }
}
