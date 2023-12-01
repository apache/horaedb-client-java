/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.sql;

import java.util.List;

import io.ceresdb.common.parser.SqlParser;
import io.ceresdb.util.Utils;
import io.ceresdb.common.util.internal.ThrowUtil;

/**
 * Parse SQL.
 *
 */
public class CeresDBParser implements SqlParser {

    private final String sql;

    private boolean             parsed;
    private StatementType       statementType = StatementType.Unknown;
    private CeresDBSqlStatement stmt;

    public CeresDBParser(String sql) {
        this.sql = sql;
    }

    @Override
    public StatementType statementType() {
        parse();

        return this.statementType;
    }

    @Override
    public List<String> tableNames() {
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
            final CeresDBSqlStatement stmt = CeresDBSqlParser.parse(this.sql)[0];

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
