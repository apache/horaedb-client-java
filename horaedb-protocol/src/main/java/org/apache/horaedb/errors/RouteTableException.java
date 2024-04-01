/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.errors;

/**
 * Get route table exception.
 *
 */
public class RouteTableException extends Exception {

    private static final long serialVersionUID = 6998981074842421829L;

    public RouteTableException() {
    }

    public RouteTableException(String message) {
        super(message);
    }

    public RouteTableException(String message, Throwable cause) {
        super(message, cause);
    }

    public RouteTableException(Throwable cause) {
        super(cause);
    }

    public RouteTableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
