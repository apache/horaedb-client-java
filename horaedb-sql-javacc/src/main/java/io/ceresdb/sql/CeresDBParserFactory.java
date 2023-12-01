/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.sql;

import io.ceresdb.common.parser.SqlParser;
import io.ceresdb.common.parser.SqlParserFactory;
import io.ceresdb.common.SPI;

@SPI(priority = 1)
public class CeresDBParserFactory implements SqlParserFactory {

    @Override
    public SqlParser getParser(final String sql) {
        return new CeresDBParser(sql);
    }
}
