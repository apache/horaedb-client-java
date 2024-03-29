/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

import org.apache.horaedb.common.OptKeys;
import org.apache.horaedb.common.util.SystemPropertyUtil;

import java.util.Collection;
import java.util.Set;

/**
 * Contains the success value of write.
 *
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
    private Set<String> tables;

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

    public Collection<String> getTables() {
        return tables;
    }

    public void setTables(Set<String> tables) {
        this.tables = tables;
    }

    public WriteOk combine(final WriteOk other) {
        this.success += other.success;
        this.failed += other.failed;
        if (this.tables == null) {
            this.tables = other.tables;
        } else if (other.tables != null) {
            this.tables.addAll(other.tables);
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
               ", tables=" + tables + //
               '}';
    }

    public static WriteOk emptyOk() {
        return ok(0, 0, null);
    }

    public static WriteOk ok(final int success, final int failed, final Set<String> tables) {
        final WriteOk ok = new WriteOk();
        ok.success = success;
        ok.failed = failed;
        ok.tables = tables;
        return ok;
    }
}
