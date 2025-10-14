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
package com.fortify.cli.common.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 *
 * @author Ruud Senden
 */
public class FcliTechnicalException extends AbstractFcliException {
    private static final long serialVersionUID = 1L;

    public FcliTechnicalException() {}
    
    public FcliTechnicalException(String fmt, Object... args) {
        super(fmt, args);
    }

    public FcliTechnicalException(String message) {
        super(message);
    }

    public FcliTechnicalException(Throwable cause) {
        super(cause);
    }

    public FcliTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
    
    @Override
    public String getStackTraceString() {
        return ExceptionUtils.getStackTrace(this);
    }
}
