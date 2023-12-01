/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.sql;

public final class SqlUtils {

    public static boolean isQuote(final char ch) {
        return ch == '"' || ch == '\'' || ch == '`';
    }

    /**
     * Escape quotes in given string.
     *
     * @param str   string
     * @param quote quote to escape
     * @return escaped string
     */
    public static String escape(final String str, final char quote) {
        if (str == null) {
            return null;
        }

        final int len = str.length();
        final StringBuilder buf = new StringBuilder(len + 10).append(quote);

        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (ch == quote || ch == '\\') {
                buf.append('\\');
            }
            buf.append(ch);
        }

        return buf.append(quote).toString();
    }

    /**
     * Unescape quoted string.
     *
     * @param str quoted string
     * @return unescaped string
     */
    @SuppressWarnings("PMD")
    public static String unescape(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        int len = str.length();
        final char quote = str.charAt(0);
        if (!isQuote(quote) || quote != str.charAt(len - 1)) { // not a quoted string
            return str;
        }

        final StringBuilder buf = new StringBuilder(len = len - 1);
        for (int i = 1; i < len; i++) {
            char ch = str.charAt(i);

            if (++i >= len) {
                buf.append(ch);
            } else {
                char nextChar = str.charAt(i);
                if (ch == '\\' || (ch == quote && nextChar == quote)) {
                    buf.append(nextChar);
                } else {
                    buf.append(ch);
                    i--;
                }
            }
        }

        return buf.toString();
    }

    private SqlUtils() {
    }
}
