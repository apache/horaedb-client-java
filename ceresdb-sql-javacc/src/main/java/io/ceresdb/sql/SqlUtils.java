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
