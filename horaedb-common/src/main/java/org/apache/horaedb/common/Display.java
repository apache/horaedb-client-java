/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common;

import java.io.PrintWriter;

/**
 * Components that implement this interface need to be able to display
 * their own state and output state information via the {@code display}
 * method.
 *
 */
public interface Display {

    /**
     * Display self state.
     *
     * @param out output
     */
    void display(final Printer out);

    interface Printer {

        /**
         * Prints an object.
         *
         * @param x The <code>Object</code> to be printed
         * @return this printer
         */
        Printer print(final Object x);

        /**
         * Prints an Object and then terminates the line.
         *
         * @param x The <code>Object</code> to be printed.
         * @return this printer
         */
        Printer println(final Object x);
    }

    class DefaultPrinter implements Display.Printer {

        private final PrintWriter out;

        public DefaultPrinter(PrintWriter out) {
            this.out = out;
        }

        @Override
        public Display.Printer print(final Object x) {
            this.out.print(x);
            return this;
        }

        @Override
        public Display.Printer println(final Object x) {
            this.out.println(x);
            return this;
        }
    }
}
