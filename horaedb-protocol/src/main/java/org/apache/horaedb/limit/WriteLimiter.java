/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.limit;

import java.util.List;

import org.apache.horaedb.models.Err;
import org.apache.horaedb.models.Point;
import org.apache.horaedb.models.Result;
import org.apache.horaedb.models.WriteOk;

/**
 * Like rust: pub type WriteLimiter = CeresDBLimiter<Collection<Rows>, Result<WriteOk, Err>>
 *
 */
public abstract class WriteLimiter extends CeresDBLimiter<List<Point>, Result<WriteOk, Err>> {

    public WriteLimiter(int maxInFlight, LimitedPolicy policy, String table) {
        super(maxInFlight, policy, table);
    }
}
