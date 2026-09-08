package com.fortify.cli.aviator.fpr.remediation.exception;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.fpr.remediation.model.SkipReason;

public class SkipRemediationException extends AviatorSimpleException {
    private static final long serialVersionUID = 1L;

    private final SkipReason reason;

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
