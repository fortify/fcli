/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
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
