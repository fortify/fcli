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
package com.fortify.cli.common.action.helper.ci.github;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;

/**
 * Exception thrown when attempting to upload SARIF reports to GitHub Code Scanning
 * but GitHub Advanced Security is not enabled/available for the repository.
 * 
 * This exception can be caught in action YAML on.fail blocks to implement fallback
 * behavior (e.g., publishing results as Check Runs for free-tier repositories).
 * 
 * @author Ruud Senden
 */
@Reflectable
public final class GhasUnavailableException extends FcliSimpleException {
    private static final long serialVersionUID = 1L;
    
    public GhasUnavailableException(String message) {
        super(message);
    }
    
    public GhasUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
