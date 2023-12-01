/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

import io.ceresdb.common.OptKeys;

/**
 * Utility for cpu.
 *
 */
public class Cpus {

    private static final int CPUS = SystemPropertyUtil.getInt(OptKeys.AVAILABLE_CPUS,
            Runtime.getRuntime().availableProcessors());

    /**
     * The configured number of available processors. The default is
     * {@link Runtime#availableProcessors()}. This can be overridden
     * by setting the system property "HoraeDB.available_cpus".
     *
     * @return available cpus num
     */
    public static int cpus() {
        return CPUS;
    }
}
