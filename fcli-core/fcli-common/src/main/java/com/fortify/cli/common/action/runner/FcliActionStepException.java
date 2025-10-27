/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.common.action.runner;

import com.fortify.cli.common.exception.FcliSimpleException;

public final class FcliActionStepException extends FcliSimpleException {
    private static final long serialVersionUID = 1L;

    public FcliActionStepException(String message, Throwable cause) {
        super(message, cause);
    }

    public FcliActionStepException(String message) {
        super(message);
    }

    public FcliActionStepException(Throwable cause) {
        super(cause);
    }
}