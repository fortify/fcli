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
package com.fortify.cli.common.exception;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.OutputHelper;

import lombok.Getter;

/**
 * Exception thrown when an fcli command execution fails with a non-zero exit code.
 * Provides access to the command's stdout and stderr output for detailed error reporting.
 */
@Reflectable
public class FcliCommandExecutionException extends FcliSimpleException {
    private static final long serialVersionUID = 1L;
    
    @Getter
    private final OutputHelper.Result result;
    
    public FcliCommandExecutionException(OutputHelper.Result result) {
        super("Command execution failed with exit code " + result.getExitCode());
        this.result = result;
    }
    
    public FcliCommandExecutionException(String message, OutputHelper.Result result) {
        super(message);
        this.result = result;
    }
}