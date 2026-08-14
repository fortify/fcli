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
package com.fortify.cli.aviator.diagnose;

import java.io.IOException;

/**
 * Thrown when HTTP CONNECT through a proxy fails (distinct from TLS handshake failures).
 */
public class AviatorProxyConnectException extends IOException {
    private static final long serialVersionUID = 1L;

    public AviatorProxyConnectException(String message) {
        super(message);
    }

    public AviatorProxyConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
