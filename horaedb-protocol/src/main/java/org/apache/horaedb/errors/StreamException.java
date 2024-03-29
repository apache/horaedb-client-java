/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.errors;

/**
 * Error about stream-query or stream-write.
 *
 */
public class StreamException extends RuntimeException {

    private static final long serialVersionUID = 2001109701785891949L;

    public StreamException() {
    }

    public StreamException(String message) {
        super(message);
    }

    public StreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamException(Throwable cause) {
        super(cause);
    }

    public StreamException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
