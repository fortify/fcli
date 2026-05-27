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
package com.fortify.cli.common.action.runner;

/**
 * Thrown by fn.yield when the consumer signals early termination (returns false).
 * Caught by the streaming function's IActionStepForEachProcessor implementation
 * to cleanly stop step execution.
 */
public final class ActionStepBreakException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ActionStepBreakException() {
        super("Break signaled by consumer");
    }
}
