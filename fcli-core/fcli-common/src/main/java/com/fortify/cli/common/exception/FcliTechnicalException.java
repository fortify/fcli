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
 * Represents an unexpected technical failure that occurred while processing a command
 * (e.g. network I/O issue, JSON parsing problem, protocol mismatch, file read error).
 * <p>Always prints the full stack trace so users and support can diagnose the underlying
 * cause. Prefer wrapping third-party or low-level exceptions at boundary layers to add
 * contextual information (e.g. which file, which endpoint) while preserving the original
 * stack via the cause.
 * <p>Do NOT use for user-correctable input problems; those belong to {@link FcliSimpleException}.
 * Use {@link FcliBugException} instead if the situation indicates a product defect or
 * unreachable code path.
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
