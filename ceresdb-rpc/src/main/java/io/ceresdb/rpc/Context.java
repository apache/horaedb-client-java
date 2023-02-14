/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.ceresdb.common.Copiable;

/**
 * Invoke context.
 *
 */
@SuppressWarnings("unchecked")
public class Context implements Copiable<Context> {

    private final Map<String, Object> ctx = new HashMap<>();

    public static Context newDefault() {
        return new Context();
    }

    public static Context of(final String key, final Object value) {
        return new Context().with(key, value);
    }

    public Context with(final String key, final Object value) {
        synchronized (this) {
            this.ctx.put(key, value);
        }
        return this;
    }

    public <T> T get(final String key) {
        synchronized (this) {
            return (T) this.ctx.get(key);
        }
    }

    public <T> T remove(final String key) {
        synchronized (this) {
            return (T) this.ctx.remove(key);
        }
    }

    public <T> T getOrDefault(final String key, final T defaultValue) {
        synchronized (this) {
            return (T) this.ctx.getOrDefault(key, defaultValue);
        }
    }

    public void clear() {
        synchronized (this) {
            this.ctx.clear();
        }
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        synchronized (this) {
            return this.ctx.entrySet();
        }
    }

    @Override
    public String toString() {
        synchronized (this) {
            return this.ctx.toString();
        }
    }

    @Override
    public Context copy() {
        synchronized (this) {
            final Context copy = new Context();
            copy.ctx.putAll(this.ctx);
            return copy;
        }
    }
}
