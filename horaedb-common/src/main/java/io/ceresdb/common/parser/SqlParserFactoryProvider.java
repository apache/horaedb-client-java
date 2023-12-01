/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.parser;

import io.ceresdb.common.util.ServiceLoader;

public class SqlParserFactoryProvider {

    private static final SqlParserFactory METRIC_PARSER_FACTORY = ServiceLoader //
            .load(SqlParserFactory.class) //
            .firstOrDefault(() -> SqlParserFactory.DEFAULT);

    public static SqlParserFactory getSqlParserFactory() {
        return METRIC_PARSER_FACTORY;
    }
}
