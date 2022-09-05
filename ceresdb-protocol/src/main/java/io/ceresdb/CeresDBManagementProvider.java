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
package io.ceresdb;

import io.ceresdb.common.util.Requires;
import io.ceresdb.common.util.ServiceLoader;

/**
 *
 * @author jiachun.fjc
 */
final class CeresDBManagementProvider {

    private static final Class<Management> MANAGEMENT_CLS;

    static {
        Class<Management> _managementCls;
        try {
            _managementCls = ServiceLoader.load(Management.class).firstClass();
        } catch (final Throwable ignored) {
            _managementCls = null;
        }
        MANAGEMENT_CLS = _managementCls;
    }

    static boolean hasManagement() {
        return MANAGEMENT_CLS != null;
    }

    static Management createManagement() {
        Requires.requireTrue(hasManagement(), "Do not have a management!");
        try {
            return MANAGEMENT_CLS.newInstance();
        } catch (final Throwable t) {
            throw new IllegalStateException("Fail to create management instance", t);
        }
    }
}