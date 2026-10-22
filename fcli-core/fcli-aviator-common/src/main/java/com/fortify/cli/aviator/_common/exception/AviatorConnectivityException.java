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

/**
 * Reports an Aviator connectivity failure with a short user message. Terminal output
 * shows only the message; DEBUG logs show only safe metadata. The original cause stays
 * attached for in-memory use, but it is never written to output or logs.
 */
public final class AviatorConnectivityException extends AviatorSimpleException {
    private static final long serialVersionUID = 1L;
    private static final int MAX_CAUSE_TYPES = 10;

    private final String kind;
    private final String operation;
    private final String statusCode;
    private final String route;
    private final String endpoint;

    /**
     * @param message safe user message
     * @param kind failure kind name
     * @param operation internal operation label
     * @param statusCode gRPC status code name
     * @param route "direct" or "proxy"
     * @param endpoint sanitized host and port of the first TCP peer
     * @param cause original failure; it is never written to output or logs
     */
    public AviatorConnectivityException(String message, String kind, String operation,
            String statusCode, String route, String endpoint, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.operation = operation;
        this.statusCode = statusCode;
        this.route = route;
        this.endpoint = endpoint;
    }

    public String getKind() {
        return kind;
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
        return String.format("Aviator connectivity failure: kind=%s, operation=%s, status=%s, route=%s, endpoint=%s, causes=[%s]",
            kind, operation, statusCode, route, endpoint, causeTypes);
    }
}
