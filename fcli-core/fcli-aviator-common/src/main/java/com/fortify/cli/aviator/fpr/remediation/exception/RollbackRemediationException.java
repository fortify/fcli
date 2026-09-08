package com.fortify.cli.aviator.fpr.remediation.exception;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;

public class RollbackRemediationException extends AviatorTechnicalException {
    private static final long serialVersionUID = 1L;

    public RollbackRemediationException(String message, Throwable cause) {
        super(message, cause);
    }
}
