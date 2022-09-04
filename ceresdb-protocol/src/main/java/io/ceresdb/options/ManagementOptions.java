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
package io.ceresdb.options;

import io.ceresdb.RouterClient;
import io.ceresdb.common.Copiable;
import io.ceresdb.common.Endpoint;
import io.ceresdb.common.Tenant;

/**
 *
 * @author jiachun.fjc
 */
public class ManagementOptions implements Copiable<ManagementOptions> {

    private Endpoint     managementAddress;
    private RouterClient routerClient;
    private Tenant       tenant;
    private boolean      checkSql = true;

    public Endpoint getManagementAddress() {
        return managementAddress;
    }

    public void setManagementAddress(Endpoint managementAddress) {
        this.managementAddress = managementAddress;
    }

    public RouterClient getRouterClient() {
        return routerClient;
    }

    public void setRouterClient(RouterClient routerClient) {
        this.routerClient = routerClient;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public boolean isCheckSql() {
        return checkSql;
    }

    public void setCheckSql(boolean checkSql) {
        this.checkSql = checkSql;
    }

    @Override
    public ManagementOptions copy() {
        final ManagementOptions opts = new ManagementOptions();
        opts.managementAddress = this.managementAddress;
        opts.routerClient = this.routerClient;
        if (this.tenant != null) {
            opts.tenant = this.tenant.copy();
        }
        opts.checkSql = this.checkSql;
        return opts;
    }

    @Override
    public String toString() {
        return "ManagementOptions{" + //
               "managementAddress=" + managementAddress + //
               "routerClient=" + routerClient + //
               ", tenant=" + tenant + //
               ", checkSql=" + checkSql + //
               '}';
    }
}
