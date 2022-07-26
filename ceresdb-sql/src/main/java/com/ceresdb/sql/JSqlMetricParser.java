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

import java.util.Collections;
import java.util.List;

import com.ceresdb.MetricParser;
import com.ceresdb.common.util.internal.ThrowUtil;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.DescribeStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;

/**
 * Parse metric QL use JSqlParser.
 *
 * @author jiachun.fjc
 */
public class JSqlMetricParser implements MetricParser {

    private final String ql;

    private boolean       parsed;
    private StatementType statementType = StatementType.Unknown;
    private Statement     stmt;

    public JSqlMetricParser(String ql) {
        this.ql = ql;
    }

    public String getQl() {
        return ql;
    }

    @Override
    public StatementType statementType() {
        parse();

        return this.statementType;
    }

    @Override
    public List<String> metricNames() {
        parse();

        switch (this.statementType) {
            case Create:
            case Select:
            case Describe:
                return SqlParserUtil.extractTableNames(this.stmt);
            case Alter:
                return Collections.singletonList(((Alter) (this.stmt)).getTable().getName());
            case Show:
                return Collections.singletonList(((ShowStatement) this.stmt).getName());
            case Drop:
                return Collections.singletonList(((Drop) this.stmt).getName().getName());
            case Insert:
                return Collections.singletonList(((Insert) this.stmt).getTable().getName());
            case Unknown:
            default:
                return reject("Invalid.statement: " + this.statementType);
        }
    }

    @Override
    public List<Column> createColumns() {
        parse();

        if (this.statementType == StatementType.Create) {
            return SqlParserUtil.extractCreateColumns((CreateTable) this.stmt);
        }

        return reject("Must be " + StatementType.Create);
    }

    private void parse() {
        if (this.parsed) {
            return;
        }

        this.parsed = true;

        try {
            final Statement stmt = CCJSqlParserUtil.parse(SqlParserUtil.amendSql(this.ql));

            if (stmt instanceof Select) {
                this.statementType = StatementType.Select;
            } else if (stmt instanceof CreateTable) {
                this.statementType = StatementType.Create;
            } else if (stmt instanceof Alter) {
                this.statementType = StatementType.Alter;
            } else if (stmt instanceof DescribeStatement) {
                this.statementType = StatementType.Describe;
            } else if (stmt instanceof ShowStatement) {
                this.statementType = StatementType.Show;
            } else if (stmt instanceof Drop) {
                this.statementType = StatementType.Drop;
            } else if (stmt instanceof Insert) {
                this.statementType = StatementType.Insert;
            } else {
                this.statementType = StatementType.Unknown;
            }

            this.stmt = stmt;
        } catch (final JSQLParserException e) {
            ThrowUtil.throwException(e);
        }
    }

    private static <T> T reject(final String msg) {
        throw new IllegalArgumentException(msg);
    }
}
