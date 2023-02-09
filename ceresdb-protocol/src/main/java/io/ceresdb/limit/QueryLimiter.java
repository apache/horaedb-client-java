/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.limit;

import io.ceresdb.models.Err;
import io.ceresdb.models.SqlQueryOk;
import io.ceresdb.models.SqlQueryRequest;
import io.ceresdb.models.Result;

/**
 * Like rust: pub type QueryLimiter = CeresDBLimiter<QueryRequest, Result<QueryOk, Err>>
 *
 */
public abstract class QueryLimiter extends CeresDBLimiter<SqlQueryRequest, Result<SqlQueryOk, Err>> {

    public QueryLimiter(int maxInFlight, LimitedPolicy policy, String metricPrefix) {
        super(maxInFlight, policy, metricPrefix);
    }
}
