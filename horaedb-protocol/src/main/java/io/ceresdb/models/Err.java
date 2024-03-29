/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

import org.apache.horaedb.common.Endpoint;
import org.apache.horaedb.common.Streamable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains the write or query error value.
 *
 */
public class Err implements Streamable<Err> {
    // error code from server
    private int code;
    // error message
    private String error;
    // the server address where the error occurred
    private Endpoint errTo;
    // the data of wrote failed, can be used to retry
    private List<Point> failedWrites;
    // other successful server results are merged here
    private WriteOk subOk;
    // the SQL failed to query
    private String failedSql;
    // the metrics of failed to query
    private Collection<String> failedTables;
    // child err merged here
    private Collection<Err> children;

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    public Endpoint getErrTo() {
        return errTo;
    }

    public Collection<Point> getFailedWrites() {
        return failedWrites;
    }

    public WriteOk getSubOk() {
        return subOk;
    }

    public String getFailedSql() {
        return failedSql;
    }

    public Collection<String> getFailedTables() {
        return failedTables;
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

    private int failedWritePointsNum() {
        return this.failedWrites == null ? 0 : this.failedWrites.size();
    }

    private Set<String> failedWriteTables() {
        return this.failedWrites == null ? Collections.emptySet() //
                : this.failedWrites.stream().map(Point::getTable).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "Err{" + //
               "code=" + code + //
               ", error='" + error + '\'' + //
               ", errTo=" + errTo + //
               ", failedWritePointsNum=" + failedWritePointsNum() + //
               ", failedWriteTables=" + failedWriteTables() + //
               ", subOk=" + subOk + //
               ", failedSql=" + failedSql + //
               ", failedTables=" + failedTables + //
               ", children=" + children + //
               '}';
    }

    public static Err writeErr(final int code, //
                               final String error, //
                               final Endpoint errTo, //
                               final List<Point> failedWrites) {
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
        err.failedSql = failedQl;
        err.failedTables = failedMetrics;
        return err;
    }
}
