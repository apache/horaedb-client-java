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
package io.ceresdb.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ceresdb.Utils;
import io.ceresdb.common.SPI;
import io.ceresdb.common.signal.FileSignal;
import io.ceresdb.common.signal.Signal;
import io.ceresdb.common.signal.SignalHandler;
import io.ceresdb.common.signal.FileSignals;

/**
 * A signal handler that can reset RW_LOGGING by {@link Utils#resetRwLogging()}.
 *
 * @author jiachun.fjc
 */
@SPI(priority = 95)
public class RwLoggingSignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RwLoggingSignalHandler.class);

    @Override
    public Signal signal() {
        return Signal.SIG_USR2;
    }

    @Override
    public void handle(final String signalName) {
        if (FileSignals.ignoreSignal(FileSignal.RwLogging)) {
            LOG.info("`RW_LOGGING`={}.", Utils.isRwLogging());
            return;
        }

        final boolean oldValue = Utils.resetRwLogging();
        LOG.info("Reset `RW_LOGGING` to {} triggered by signal: {}.", !oldValue, signalName);
    }
}
