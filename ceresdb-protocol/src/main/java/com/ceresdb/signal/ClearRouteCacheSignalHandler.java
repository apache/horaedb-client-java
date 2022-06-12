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
package com.ceresdb.signal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceresdb.CeresDBxClient;
import com.ceresdb.common.SPI;
import com.ceresdb.common.signal.FileSignal;
import com.ceresdb.common.signal.FileSignals;
import com.ceresdb.common.signal.SignalHandler;

/**
 * A signal handler that can clear the route cache.
 *
 * @author jiachun.fjc
 */
@SPI(priority = 94)
public class ClearRouteCacheSignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ClearRouteCacheSignalHandler.class);

    @Override
    public void handle(final String signalName) {
        if (FileSignals.ignoreSignal(FileSignal.ClearCache)) {
            return;
        }

        LOG.info("Clear all route cache triggered by signal: {}.", signalName);

        final List<CeresDBxClient> instances = CeresDBxClient.instances();
        if (instances.isEmpty()) {
            return;
        }

        for (final CeresDBxClient ins : instances) {
            final int count = ins.routerClient().clearRouteCache();
            LOG.info("Clearing the client route cache, count={}, client id={}.", count, ins.id());
        }
    }
}
