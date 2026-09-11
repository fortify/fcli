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

import java.util.List;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.fpr.remediation.writer.RollbackFileWrite;

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
