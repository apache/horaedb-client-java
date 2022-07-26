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
package com.ceresdb.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 *
 * @author jiachun.fjc
 */
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
