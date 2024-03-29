/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.signal;

import java.io.File;
import java.nio.file.Paths;

public class FileSignals {

    private static final String[] EMPTY_ARRAY = new String[0];

    public static boolean ignoreSignal(final FileSignal fileSignal) {
        return !Paths.get(FileOutputHelper.getOutDir(), fileSignal.getFilename()).toFile().exists();
    }

    public static boolean ignoreFileOutputSignal() {
        return list().length > 0;
    }

    public static String[] list() {
        final File dir = new File(FileOutputHelper.getOutDir());
        if (!dir.exists() || !dir.isDirectory()) {
            return EMPTY_ARRAY;
        }
        return dir.list((d, name) -> FileSignal.parse(name).isPresent());
    }
}
