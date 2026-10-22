/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.aviator._common.exception;

import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

/** Keeps transport cause text out of CLI output and DEBUG logs. */
public final class AviatorConnectivityException extends AviatorSimpleException {
    private static final long serialVersionUID = 1L;
    private static final int MAX_CAUSE_TYPES = 10;

    private final String kind;

    /** Message and kind must be safe for display. */
    public AviatorConnectivityException(String message, String kind, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }

    @Override
    public boolean isCauseSensitive() {
        return true;
    }

    @Override
    public String getStackTraceString() {
        return getMessage();
    }

    @Override
    public String getSafeDiagnosticString() {
        var causeTypes = ExceptionUtils.getThrowableList(getCause()).stream()
            .limit(MAX_CAUSE_TYPES)
            .map(t -> t.getClass().getName())
            .collect(Collectors.joining(" <- "));
        return String.format("Aviator connectivity failure: kind=%s, message=%s, causes=[%s]",
            kind, getMessage().replace("\n", " / "), causeTypes);
    }
}
