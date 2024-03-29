/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.sql;

import io.ceresdb.common.parser.SqlParser;
import io.ceresdb.common.parser.SqlParserFactory;
import io.ceresdb.common.SPI;

@SPI
public class JSqlParserFactory implements SqlParserFactory {

    @Override
    public SqlParser getParser(final String sql) {
        return new JSqlParser(sql);
    }
}
