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

/**
 * Aviator connection failure with a message that is safe to show.
 * <p>
 * The transport cause is not attached: its text can contain proxy credentials or
 * host details, and fcli prints and logs the cause chain of simple exceptions.
 */
public final class AviatorConnectivityException extends AviatorSimpleException {
    private static final long serialVersionUID = 1L;

    public AviatorConnectivityException(String message) {
        super(message);
    }
}