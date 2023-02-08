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
// Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
package io.ceresdb.signal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ceresdb.CeresDBClient;
import io.ceresdb.common.Display;
import io.ceresdb.common.SPI;
import io.ceresdb.common.signal.FileOutputHelper;
import io.ceresdb.common.signal.FileSignals;
import io.ceresdb.common.signal.SignalHandler;
import io.ceresdb.common.util.Files;

/**
 * A signal handler that can display all client instance's memory state.
 *
 */
@SPI(priority = 98)
public class DisplaySignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DisplaySignalHandler.class);

    private static final String BASE_NAME = "CeresDB_client_display.log";

    @Override
    public void handle(final String signalName) {
        if (FileSignals.ignoreFileOutputSignal()) {
            return;
        }

        final List<CeresDBClient> instances = CeresDBClient.instances();
        if (instances.isEmpty()) {
            return;
        }

        try {
            final File file = FileOutputHelper.getOutputFile(BASE_NAME);

            LOG.info("Displaying CeresDB clients triggered by signal: {} to file: {}.", signalName,
                    file.getAbsoluteFile());

            try (PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                final Display.Printer printer = new Display.DefaultPrinter(out);
                for (final CeresDBClient ins : instances) {
                    ins.display(printer);
                }
                out.flush();
            }
            Files.fsync(file);
        } catch (final IOException e) {
            LOG.error("Fail to display CeresDB clients: {}.", instances, e);
        }
    }
}
