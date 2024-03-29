/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.errors;

/**
 * Iterator timeout error on stream query.
 *
 */
public class IteratorException extends RuntimeException {

    private static final long serialVersionUID = 8903401089686818937L;

    public IteratorException() {
    }

    public IteratorException(String message) {
        super(message);
    }

    public IteratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public IteratorException(Throwable cause) {
        super(cause);
    }

    public IteratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
