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

/**
 * Signals a product defect or logically impossible state (broken invariant, unreachable branch
 * reached, internal misuse of an API). Printed output includes a full stack trace (inherited
 * from {@link FcliTechnicalException}) to aid rapid diagnosis. Messages should explicitly guide
 * the user to file a bug report if the issue persists and reference the unexpected condition.
 * <p>Only use when the user cannot resolve the problem through different input or configuration.
 * If the condition could be triggered by invalid user input, prefer {@link FcliSimpleException}.
 * For external transient failures (network, parse errors) prefer {@link FcliTechnicalException}.
 *
 * @author Ruud Senden
 */
public class FcliBugException extends FcliTechnicalException {
    private static final long serialVersionUID = 1L;

    public FcliBugException() {}
    
    public FcliBugException(String fmt, Object... args) {
        super(fmt, args);
    }

    public FcliBugException(String message) {
        super(message);
    }

    public FcliBugException(Throwable cause) {
        super(cause);
    }

    public FcliBugException(String message, Throwable cause) {
        super(message, cause);
    }
}
