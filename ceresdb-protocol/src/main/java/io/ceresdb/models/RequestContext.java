/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

/**
 *
 * RequestContext of business model, such as database
 */
public class RequestContext {
    private String database;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
