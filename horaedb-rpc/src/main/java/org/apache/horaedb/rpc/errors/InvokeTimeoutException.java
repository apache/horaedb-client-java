/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc.errors;

public class InvokeTimeoutException extends RemotingException {

    private static final long serialVersionUID = -4710810309766380565L;

    public InvokeTimeoutException() {
    }

    public InvokeTimeoutException(String message) {
        super(message);
    }

    public InvokeTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvokeTimeoutException(Throwable cause) {
        super(cause);
    }

    public InvokeTimeoutException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
