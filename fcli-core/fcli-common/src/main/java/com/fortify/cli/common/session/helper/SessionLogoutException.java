package com.fortify.cli.common.session.helper;

import lombok.Getter;

public class SessionLogoutException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    @Getter private boolean destroySession;
    public SessionLogoutException(String message, boolean destroySession) {
        this(message, null, destroySession);
    }

    public SessionLogoutException(Throwable cause, boolean destroySession) {
        this(null, cause, destroySession);
    }

    public SessionLogoutException(String message, Throwable cause, boolean destroySession) {
        super(message, cause);
        this.destroySession = destroySession;
    }
}
