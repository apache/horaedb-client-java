/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.sql;

import org.apache.horaedb.common.SPI;
import org.apache.horaedb.common.parser.SqlParser;
import org.apache.horaedb.common.parser.SqlParserFactory;

@SPI(priority = 1)
public class CeresDBParserFactory implements SqlParserFactory {

    @Override
    public SqlParser getParser(final String sql) {
        return new CeresDBParser(sql);
    }
}
