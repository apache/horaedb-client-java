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
package com.ceresdb.options;

import java.util.concurrent.Executor;

import com.ceresdb.LimitedPolicy;
import com.ceresdb.RouterClient;
import com.ceresdb.common.Copiable;

/**
 * Write options.
 *
 * @author jiachun.fjc
 */
public class WriteOptions implements Copiable<WriteOptions> {

    private RouterClient routerClient;
    private Executor     asyncPool;

    // Maximum data entry per write
    private int maxRetries = 1;
    // In the case of routing table failure or some other retry able error, a retry of the write is attempted.
    private int maxWriteSize = 512;
    // Write flow limit: maximum number of data rows in-flight.
    private int           maxInFlightWriteRows = 8192;
    private LimitedPolicy limitedPolicy        = LimitedPolicy.defaultWriteLimitedPolicy();

    public RouterClient getRoutedClient() {
        return routerClient;
    }

    public void setRoutedClient(RouterClient routerClient) {
        this.routerClient = routerClient;
    }

    public Executor getAsyncPool() {
        return asyncPool;
    }

    public void setAsyncPool(Executor asyncPool) {
        this.asyncPool = asyncPool;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxWriteSize() {
        return maxWriteSize;
    }

    public void setMaxWriteSize(int maxWriteSize) {
        this.maxWriteSize = maxWriteSize;
    }

    public int getMaxInFlightWriteRows() {
        return maxInFlightWriteRows;
    }

    public void setMaxInFlightWriteRows(int maxInFlightWriteRows) {
        this.maxInFlightWriteRows = maxInFlightWriteRows;
    }

    public LimitedPolicy getLimitedPolicy() {
        return limitedPolicy;
    }

    public void setLimitedPolicy(LimitedPolicy limitedPolicy) {
        this.limitedPolicy = limitedPolicy;
    }

    @Override
    public WriteOptions copy() {
        final WriteOptions opts = new WriteOptions();
        opts.routerClient = this.routerClient;
        opts.asyncPool = this.asyncPool;
        opts.maxRetries = this.maxRetries;
        opts.maxWriteSize = this.maxWriteSize;
        opts.maxInFlightWriteRows = this.maxInFlightWriteRows;
        opts.limitedPolicy = this.limitedPolicy;
        return opts;
    }

    @Override
    public String toString() {
        return "WriteOptions{" + //
               "routerClient=" + routerClient + //
               ", globalAsyncPool=" + asyncPool + //
               ", maxRetries=" + maxRetries + //
               ", maxWriteSize=" + maxWriteSize + //
               ", maxInFlightWriteRows=" + maxInFlightWriteRows + //
               ", limitedPolicy=" + limitedPolicy + //
               '}';
    }
}
