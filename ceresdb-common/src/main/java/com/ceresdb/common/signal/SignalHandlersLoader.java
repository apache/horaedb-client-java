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
package com.ceresdb.common.signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceresdb.common.OptKeys;
import com.ceresdb.common.util.ServiceLoader;
import com.ceresdb.common.util.SystemPropertyUtil;

/**
 * A tool class for loading and registering all signals.
 *
 * Do not support windows.
 *
 * @author jiachun.fjc
 */
public class SignalHandlersLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SignalHandlersLoader.class);

    private static final boolean USE_OS_SIGNAL = SystemPropertyUtil.getBool(OptKeys.USE_OS_SIGNAL, true);

    /**
     * Load and register all signals.
     */
    public static void load() {
        try {
            if (USE_OS_SIGNAL && SignalHelper.supportSignal()) {
                final List<SignalHandler> handlers = ServiceLoader.load(SignalHandler.class) //
                        .sort();

                LOG.info("Loaded signals: {}.", handlers);

                final Map<Signal, List<SignalHandler>> mapTo = new HashMap<>();
                handlers.forEach(h -> mapTo.computeIfAbsent(h.signal(), sig -> new ArrayList<>()) //
                        .add(h));

                mapTo.forEach((sig, hs) -> {
                    final boolean success = SignalHelper.addSignal(sig, hs);
                    LOG.info("Add signal [{}] handler {} {}.", sig, hs, success ? "success" : "failed");
                });
            }
        } catch (final Throwable t) {
            LOG.error("Fail to add signal.", t);
        }
    }
}
