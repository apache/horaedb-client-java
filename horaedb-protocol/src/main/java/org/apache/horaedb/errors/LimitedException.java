/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.errors;

/**
 * A flow control exception that can be thrown by AbortPolicy.
 *
 */
public class LimitedException extends RuntimeException {

    private static final long serialVersionUID = 343058282792575149L;

    public LimitedException() {
    }

    public LimitedException(String message) {
        super(message);
    }

    public LimitedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimitedException(Throwable cause) {
        super(cause);
    }

    public LimitedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
