/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

/**
 * Reuse {@link StringBuilder} based on {@link ThreadLocal}.
 *
 * Be careful that do not to nest in the same thread.
 *
 */
public class StringBuilderHelper {

    private static final int                              MAX_BUF_SIZE;
    private static final ThreadLocal<StringBuilderHolder> HOLDER_THREAD_LOCAL;

    static {
        MAX_BUF_SIZE = 1024 << 3; // 8k
        HOLDER_THREAD_LOCAL = ThreadLocal.withInitial(StringBuilderHolder::new);
    }

    public static StringBuilder get() {
        final StringBuilderHolder holder = HOLDER_THREAD_LOCAL.get();
        return holder.getStringBuilder();
    }

    public static void truncate() {
        final StringBuilderHolder holder = HOLDER_THREAD_LOCAL.get();
        holder.truncate();
    }

    private static class StringBuilderHolder {

        private StringBuilder buf = new StringBuilder();

        private StringBuilder getStringBuilder() {
            truncate();
            return this.buf;
        }

        private void truncate() {
            if (this.buf.capacity() < MAX_BUF_SIZE) {
                this.buf.setLength(0); // reuse
            } else {
                this.buf = new StringBuilder();
            }
        }
    }
}
