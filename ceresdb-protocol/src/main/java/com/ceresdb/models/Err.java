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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ceresdb.common.Endpoint;
import com.ceresdb.common.Streamable;

/**
 * Contains the write or query error value.
 *
 * @author jiachun.fjc
 */
public class Err implements Streamable<Err> {
    // error code from server
    private int                code;
    // error message
    private String             error;
    // the server address where the error occurred
    private Endpoint           errTo;
    // the data of wrote failed, can be used to retry
    private Collection<Rows>   failedWrites;
    // other successful server results are merged here
    private WriteOk            subOk;
    // the QL failed to query
    private String             failedQl;
    // the metrics of failed to query
    private Collection<String> failedMetrics;
    // child err merged here
    private Collection<Err>    children;

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    public Endpoint getErrTo() {
        return errTo;
    }

    public Collection<Rows> getFailedWrites() {
        return failedWrites;
    }

    public WriteOk getSubOk() {
        return subOk;
    }

    public String getFailedQl() {
        return failedQl;
    }

    public Collection<String> getFailedMetrics() {
        return failedMetrics;
    }

    public Err combine(final Err err) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(err);
        return this;
    }

    public Err combine(final WriteOk subOk) {
        if (this.subOk == null) {
            this.subOk = subOk;
        } else {
            this.subOk.combine(subOk);
        }
        return this;
    }

    public <T> Result<T, Err> mapToResult() {
        return Result.err(this);
    }

    @Override
    public Stream<Err> stream() {
        final Stream<Err> first = Stream.of(this);
        if (this.children == null || this.children.isEmpty()) {
            return first;
        } else {
            return Stream.concat(first, this.children.stream());
        }
    }

    private int failedWriteRowsNum() {
        return this.failedWrites == null ? 0 : this.failedWrites.size();
    }

    private List<String> failedWriteMetrics() {
        return this.failedWrites == null ? Collections.emptyList() //
                : this.failedWrites.stream().map(Rows::getMetric).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Err{" + //
               "code=" + code + //
               ", error='" + error + '\'' + //
               ", errTo=" + errTo + //
               ", failedWriteRowsNum=" + failedWriteRowsNum() + //
               ", failedWriteMetrics=" + failedWriteMetrics() + //
               ", subOk=" + subOk + //
               ", failedQl=" + failedQl + //
               ", failedMetrics=" + failedMetrics + //
               ", children=" + children + //
               '}';
    }

    public static Err writeErr(final int code, //
                               final String error, //
                               final Endpoint errTo, //
                               final Collection<Rows> failedWrites) {
        final Err err = new Err();
        err.code = code;
        err.error = error;
        err.errTo = errTo;
        err.failedWrites = failedWrites;
        return err;
    }

    public static Err queryErr(final int code, //
                               final String error, //
                               final Endpoint errTo, //
                               final String failedQl, //
                               final Collection<String> failedMetrics) {
        final Err err = new Err();
        err.code = code;
        err.error = error;
        err.errTo = errTo;
        err.failedQl = failedQl;
        err.failedMetrics = failedMetrics;
        return err;
    }
}
