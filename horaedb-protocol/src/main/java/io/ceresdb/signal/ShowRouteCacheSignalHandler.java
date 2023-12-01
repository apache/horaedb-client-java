/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.signal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.ceresdb.HoraeDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ceresdb.common.Display;
import io.ceresdb.common.SPI;
import io.ceresdb.common.signal.FileOutputHelper;
import io.ceresdb.common.signal.FileSignals;
import io.ceresdb.common.signal.SignalHandler;
import io.ceresdb.common.util.Files;

/**
 * A signal handler that can log all of the route cache items to file.
 *
 */
@SPI(priority = 96)
public class ShowRouteCacheSignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ShowRouteCacheSignalHandler.class);

    private static final String BASE_NAME = "CeresDB_route_cache.log";

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

            LOG.info("Logging all of the route cache items triggered by signal: {} to file: {}.", signalName,
                    file.getAbsoluteFile());

            try (PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                final Display.Printer printer = new Display.DefaultPrinter(out);
                for (final HoraeDBClient ins : instances) {
                    printer.print("clientId=").println(ins.id());
                    ins.routerClient().iterator().forEachRemaining(printer::println);
                }
                out.flush();
            }
            Files.fsync(file);
        } catch (final IOException e) {
            LOG.error("Fail to log the route cache items: {}.", instances, e);
        }
    }
}
