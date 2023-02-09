/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.limit;

import java.util.List;

import io.ceresdb.models.Err;
import io.ceresdb.models.Point;
import io.ceresdb.models.Result;
import io.ceresdb.models.WriteOk;

/**
 * Like rust: pub type WriteLimiter = CeresDBLimiter<Collection<Rows>, Result<WriteOk, Err>>
 *
 */
public abstract class WriteLimiter extends CeresDBLimiter<List<Point>, Result<WriteOk, Err>> {

    public WriteLimiter(int maxInFlight, LimitedPolicy policy, String table) {
        super(maxInFlight, policy, table);
    }
}
