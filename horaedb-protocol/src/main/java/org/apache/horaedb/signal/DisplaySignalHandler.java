/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.signal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.HoraeDBClient;
import org.apache.horaedb.common.Display;
import org.apache.horaedb.common.SPI;
import org.apache.horaedb.common.signal.FileOutputHelper;
import org.apache.horaedb.common.signal.FileSignals;
import org.apache.horaedb.common.signal.SignalHandler;
import org.apache.horaedb.common.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        final List<HoraeDBClient> instances = HoraeDBClient.instances();
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
                for (final HoraeDBClient ins : instances) {
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
