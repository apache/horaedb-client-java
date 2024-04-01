/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.limit;

import org.apache.horaedb.models.Err;
import org.apache.horaedb.models.SqlQueryOk;
import org.apache.horaedb.models.SqlQueryRequest;
import org.apache.horaedb.models.Result;

/**
 * Like rust: pub type QueryLimiter = CeresDBLimiter<QueryRequest, Result<QueryOk, Err>>
 *
 */
public abstract class QueryLimiter extends CeresDBLimiter<SqlQueryRequest, Result<SqlQueryOk, Err>> {

    public QueryLimiter(int maxInFlight, LimitedPolicy policy, String metricPrefix) {
        super(maxInFlight, policy, metricPrefix);
    }
}
