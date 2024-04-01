/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Files {

    /**
     * Calls fsync on a file or directory.
     *
     * @param file file or directory
     * @throws IOException if an I/O error occurs
     */
    public static void fsync(final File file) throws IOException {
        final boolean isDir = file.isDirectory();
        // Can't fsync on windows.
        if (isDir && Platform.isWindows()) {
            return;
        }

        try (FileChannel fc = FileChannel.open(file.toPath(),
                isDir ? StandardOpenOption.READ : StandardOpenOption.WRITE)) {
            fc.force(true);
        }
    }

    /**
     * Creates the directory named by this pathname if not exists.
     *
     * @param path pathname
     */
    public static void mkdirIfNotExists(final String path) throws IOException {
        final File dir = Paths.get(path).toFile().getAbsoluteFile();
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new IOException(
                        "File " + dir + " exists and is " + "not a directory. Unable to create directory.");
            }
        } else if (!dir.mkdirs() && !dir.isDirectory()) {
            // Double-check that some other thread or process hasn't made
            // the directory in the background
            throw new IOException("Unable to create directory " + dir);
        }
    }
}
