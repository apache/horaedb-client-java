/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.signal;

import org.apache.horaedb.common.SPI;
import org.apache.horaedb.common.signal.FileOutputHelper;
import org.apache.horaedb.common.signal.FileSignal;
import org.apache.horaedb.common.signal.FileSignals;
import org.apache.horaedb.common.signal.SignalHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SPI(priority = 99)
public class ShowHelpSignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ShowHelpSignalHandler.class);

    @Override
    public void handle(final String signalName) {
        final String outDir = FileOutputHelper.getOutDir();
        LOG.info("-- CeresDBClient Signal Help --");
        LOG.info("    Signal output dir: {}", outDir);
        for (final FileSignal fileSignal : FileSignal.values()) {
            formatLog(outDir, fileSignal);
        }
        LOG.info("    How to get metrics、display info and route cache detail:");
        LOG.info("      [1] `cd {}`", outDir);
        LOG.info("      [2] `rm *.sig`");
        LOG.info("      [3] `kill -s SIGUSR2 $pid`");
        LOG.info("");
        LOG.info("    The file signals that is currently open:");
        for (final String f : FileSignals.list()) {
            LOG.info("      {}", f);
        }
        LOG.info("");
    }

    private static void formatLog(final String outDir, final FileSignal fileSignal) {
        LOG.info("");
        LOG.info("    {}:", fileSignal.getComment());
        LOG.info("      [1] `cd {}`", outDir);
        LOG.info("      [2] `touch {}`", fileSignal.getFilename());
        LOG.info("      [3] `kill -s SIGUSR2 $pid`");
        LOG.info("      [4] `rm {}`", fileSignal.getFilename());
        LOG.info("");
    }
}
