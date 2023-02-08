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
// Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
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
