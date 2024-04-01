/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.parser;

public interface SqlParserFactory {

    SqlParserFactory DEFAULT = new NoopFactory();

    SqlParser getParser(final String sql);

    class NoopFactory implements SqlParserFactory {

        @Override
        public SqlParser getParser(final String sql) {
            return SqlParser.DEFAULT;
        }
    }
}
