/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc.errors;

/**
 * Takes some additional error message, no stack trace, can be added to an exists Exception.
 *
 */
public class OnlyErrorMessage extends Exception {

    private static final long serialVersionUID = -2814594738496339674L;

    public OnlyErrorMessage(String message) {
        super(message);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
