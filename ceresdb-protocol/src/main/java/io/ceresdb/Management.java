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
package io.ceresdb;

import io.ceresdb.common.Display;
import io.ceresdb.common.Lifecycle;
import io.ceresdb.models.SqlResult;
import io.ceresdb.options.ManagementOptions;
import io.ceresdb.rpc.Context;

/**
 * CeresDB client for management API.
 *
 * @author jiachun.fjc
 */
public interface Management extends Lifecycle<ManagementOptions>, Display {

    /**
     * @see #executeSql(boolean, Context, String, Object...)
     */
    default SqlResult executeSql(final String fmtSql, final Object... args) {
        return executeSql(true, Context.newDefault(), fmtSql, args);
    }

    /**
     * Execute a SQL.
     *
     * CREATE TABLE:
     * ```
     * CREATE TABLE my_first_table(
     *     ts TIMESTAMP NOT NULL,
     *     c1 STRING TAG NOT NULL,
     *     c2 STRING TAG NOT NULL,
     *     c3 DOUBLE NULL,
     *     c4 STRING NULL,
     *     c5 INT64 NULL,
     *     c6 FLOAT NULL,
     *     c7 INT32 NULL,
     *     c8 INT16 NULL,
     *     c9 INT8 NULL,
     *     c10 BOOLEAN NULL,
     *     c11 UINT64 NULL,
     *     c12 UINT32 NULL,
     *     c13 UINT16 NULL,
     *     c14 UINT8 NULL,
     *     c15 TIMESTAMP NULL,
     *     c16 VARBINARY NULL,
     *     TIMESTAMP KEY(ts)
     * ) ENGINE=Analytic
     * ```
     *
     * DESCRIBE TABLE:
     * ```
     * DESCRIBE TABLE my_table
     * ```
     *
     * @param autoRouting automatic routing to server
     * @param ctx         the invoke context, will automatically be placed in HTTP Headers
     * @param fmtSql      format sql string
     * @param args        arguments referenced by the format specifiers in the format
     *                    sql string.  If there are more arguments than format specifiers,
     *                    the extra arguments are ignored.  The number of arguments is
     *                    variable and may be zero.
     * @return the sql result.
     */
    SqlResult executeSql(final boolean autoRouting, final Context ctx, final String fmtSql, final Object... args);
}
