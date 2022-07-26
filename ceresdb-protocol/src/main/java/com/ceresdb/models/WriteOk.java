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
package com.ceresdb.models;

import java.util.Collection;

import com.ceresdb.common.OptKeys;
import com.ceresdb.common.util.SystemPropertyUtil;

/**
 * Contains the success value of write.
 *
 * @author jiachun.fjc
 */
public class WriteOk {

    private static final boolean COLLECT_WROTE_DETAIL = SystemPropertyUtil.getBool(OptKeys.COLLECT_WROTE_DETAIL, false);

    public static boolean isCollectWroteDetail() {
        return COLLECT_WROTE_DETAIL;
    }

    private int success;
    private int failed;

    /**
     * Empty if {@link #COLLECT_WROTE_DETAIL == false}.
     */
    private Collection<String> metrics;

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public Collection<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(Collection<String> metrics) {
        this.metrics = metrics;
    }

    public WriteOk combine(final WriteOk other) {
        this.success += other.success;
        this.failed += other.failed;
        if (this.metrics == null) {
            this.metrics = other.metrics;
        } else if (other.metrics != null) {
            this.metrics.addAll(other.metrics);
        }
        return this;
    }

    public Result<WriteOk, Err> mapToResult() {
        return Result.ok(this);
    }

    @Override
    public String toString() {
        return "WriteOk{" + //
               "success=" + success + //
               ", failed=" + failed + //
               ", metrics=" + metrics + //
               '}';
    }

    public static WriteOk emptyOk() {
        return ok(0, 0, null);
    }

    public static WriteOk ok(final int success, final int failed, final Collection<String> metrics) {
        final WriteOk ok = new WriteOk();
        ok.success = success;
        ok.failed = failed;
        ok.metrics = metrics;
        return ok;
    }
}
