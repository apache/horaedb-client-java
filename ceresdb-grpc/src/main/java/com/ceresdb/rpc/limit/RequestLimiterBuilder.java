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
package com.ceresdb.rpc.limit;

import java.time.Duration;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limiter.AbstractPartitionedLimiter;
import com.netflix.concurrency.limits.limiter.BlockingLimiter;

/**
 * Refer to `concurrency-limit-grpc`
 *
 * @author jiachun.fjc
 */
public class RequestLimiterBuilder extends AbstractPartitionedLimiter.Builder<RequestLimiterBuilder, RequestLimitCtx> {

    public static RequestLimiterBuilder newBuilder() {
        return new RequestLimiterBuilder();
    }

    private boolean blockOnLimit = true;
    private long    blockTimeoutMillis;

    public RequestLimiterBuilder partitionByMethod() {
        return super.partitionResolver(RequestLimitCtx::partitionKey);
    }

    /**
     * When set to true new calls to the channel will block when the limit has been
     * reached instead of failing fast with an UNAVAILABLE status.
     *
     * @param blockOnLimit       whether block on limit has been reached
     * @param blockTimeoutMillis max block time on limit
     * @return this builder
     */
    public RequestLimiterBuilder blockOnLimit(final boolean blockOnLimit, final long blockTimeoutMillis) {
        this.blockOnLimit = blockOnLimit;
        this.blockTimeoutMillis = blockTimeoutMillis;
        return this;
    }

    public Limiter<RequestLimitCtx> build() {
        return this.blockOnLimit ? BlockingLimiter.wrap(super.build(), Duration.ofMillis(this.blockTimeoutMillis)) :
                super.build();
    }

    @Override
    protected RequestLimiterBuilder self() {
        return this;
    }
}
