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
package com.fortify.cli.aviator.fpr.remediation.exception;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.fpr.remediation.SkipReason;

public class SkipRemediationException extends AviatorSimpleException {
    private static final long serialVersionUID = 1L;

    public final SkipReason reason;

    public SkipRemediationException(SkipReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public SkipRemediationException(SkipReason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public SkipReason reason() {
        return reason;
    }
}
