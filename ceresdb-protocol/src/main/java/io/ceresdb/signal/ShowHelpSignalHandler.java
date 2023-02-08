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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ceresdb.common.SPI;
import io.ceresdb.common.signal.FileOutputHelper;
import io.ceresdb.common.signal.FileSignal;
import io.ceresdb.common.signal.FileSignals;
import io.ceresdb.common.signal.SignalHandler;

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
