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
 * Exception thrown when quota-based filtering results in zero issues to audit.
 * This is not a technical error but a business logic outcome indicating that
 * all issues were filtered out based on quota and priority constraints.
 */
public class AviatorQuotaFilterException extends AviatorSimpleException {
    public AviatorQuotaFilterException(String message) {
        super(message);
    }
}
