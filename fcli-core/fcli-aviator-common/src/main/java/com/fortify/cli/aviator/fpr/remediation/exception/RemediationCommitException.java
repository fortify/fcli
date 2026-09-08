package com.fortify.cli.aviator.fpr.remediation.exception;

import java.util.List;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.fpr.remediation.write.RollbackFileWrite;

public class RemediationCommitException extends AviatorTechnicalException {
    private static final long serialVersionUID = 1L;

    private final List<RollbackFileWrite> rollbacks;

    public RemediationCommitException(String message, Throwable cause, List<RollbackFileWrite> rollbacks) {
        super(message, cause);
        this.rollbacks = rollbacks;
    }

    public List<RollbackFileWrite> getRollbacks() {
        return rollbacks;
    }
}
