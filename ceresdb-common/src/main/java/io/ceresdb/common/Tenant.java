/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
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
