package com.fortify.cli.common.session.helper;

import lombok.Getter;

public class FcliSessionLogoutException extends FcliSessionException {
    private static final long serialVersionUID = 1L;
    @Getter private boolean destroySession;
    public FcliSessionLogoutException(String message, boolean destroySession) {
        this(message, null, destroySession);
    }

    public FcliSessionLogoutException(Throwable cause, boolean destroySession) {
        this(null, cause, destroySession);
    }

    public FcliSessionLogoutException(String message, Throwable cause, boolean destroySession) {
        super(message, cause);
        this.destroySession = destroySession;
    }
}
