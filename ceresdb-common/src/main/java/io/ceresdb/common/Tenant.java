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
package io.ceresdb.common;

/**
 * A tenant for CeresDB.
 *
 */
public final class Tenant implements Copiable<Tenant> {

    private final String tenant;
    // Default subtenant, which is used if you do not re-specify a
    // subtenant each time you make a call.
    private final String childTenant;
    // Don't tell the secret to anyone, heaven knows and earth knows,
    // you know and I know.  ＼（＾▽＾）／
    private final String token;

    private Tenant(String tenant, String childTenant, String token) {
        this.tenant = tenant;
        this.childTenant = childTenant;
        this.token = token;
    }

    public static Tenant of(final String tenant, final String childTenant, final String token) {
        return new Tenant(tenant, childTenant, token);
    }

    public String getTenant() {
        return tenant;
    }

    public String getChildTenant() {
        return childTenant;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Tenant copy() {
        return of(this.tenant, this.childTenant, this.token);
    }

    @Override
    public String toString() {
        return "Tenant{" + //
               "tenant='" + tenant + '\'' + //
               ", childTenant='" + childTenant + '\'' + //
               '}';
    }
}
