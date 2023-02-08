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
package io.ceresdb.rpc.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ceresdb.common.SPI;
import io.ceresdb.common.signal.FileSignal;
import io.ceresdb.common.signal.FileSignals;
import io.ceresdb.common.signal.SignalHandler;
import io.ceresdb.rpc.interceptors.ClientRequestLimitInterceptor;

/**
 * A signal handler that can reset LIMIT_SWITCH by {@link ClientRequestLimitInterceptor#resetLimitSwitch()}.
 *
 */
@SPI(priority = 89)
public class RpcLimitSignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RpcLimitSignalHandler.class);

    @Override
    public void handle(final String signalName) {
        if (FileSignals.ignoreSignal(FileSignal.RpcLimit)) {
            LOG.info("`LIMIT_SWITCH`={}.", ClientRequestLimitInterceptor.isLimitSwitchOpen());
            return;
        }

        final boolean oldValue = ClientRequestLimitInterceptor.resetLimitSwitch();
        LOG.warn("Reset `LIMIT_SWITCH` to {} triggered by signal: {}.", !oldValue, signalName);
    }
}
