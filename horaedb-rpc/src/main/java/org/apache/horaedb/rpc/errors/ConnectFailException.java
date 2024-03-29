/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.rpc.errors;

public class ConnectFailException extends RemotingException {

    private static final long serialVersionUID = 3129127065579018606L;

    public ConnectFailException() {
    }

    public ConnectFailException(String message) {
        super(message);
    }

    public ConnectFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectFailException(Throwable cause) {
        super(cause);
    }

    public ConnectFailException(String message, Throwable cause, boolean enableSuppression,
                                boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
