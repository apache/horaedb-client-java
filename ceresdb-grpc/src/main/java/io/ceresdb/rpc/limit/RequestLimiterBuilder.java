/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.rpc.limit;

import java.time.Duration;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limiter.AbstractPartitionedLimiter;
import com.netflix.concurrency.limits.limiter.BlockingLimiter;

/**
 * Refer to `concurrency-limit-grpc`
 *
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
