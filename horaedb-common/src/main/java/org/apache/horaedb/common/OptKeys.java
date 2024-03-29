/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common;

/**
 * System properties option keys
 *
 */
public final class OptKeys {

    public static final String OS_NAME                   = "os.name";
    public static final String RW_LOGGING                = "HoraeDB.client.read.write.rw_logging";
    public static final String COLLECT_WROTE_DETAIL      = "HoraeDB.client.write.collect_wrote_detail";
    public static final String USE_OS_SIGNAL             = "HoraeDB.client.use_os_signal";
    public static final String REPORT_PERIOD             = "HoraeDB.reporter.period_minutes";
    public static final String SIG_OUT_DIR               = "HoraeDB.signal.out_dir";
    public static final String GRPC_CONN_RESET_THRESHOLD = "HoraeDB.grpc.conn.failures.reset_threshold";
    public static final String AVAILABLE_CPUS            = "HoraeDB.available_cpus";
    public static final String WRITE_LIMIT_PERCENT       = "HoraeDB.rpc.write.limit_percent";

    private OptKeys() {
    }
}
