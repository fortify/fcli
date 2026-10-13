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
package com.fortify.cli.aviator.ssc.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;

import picocli.CommandLine;

class AviatorSSCCorrelateSastDastCommandTest {
    @Test
    void acceptsSscRefreshOptions() {
        var commandLine = new CommandLine(new AviatorSSCCorrelateSastDastCommand());

        var parseResult = commandLine.parseArgs(
            "--av", "test:1.0", "--no-refresh", "--refresh-timeout", "2m");

        assertFalse(parseResult.matchedOptionValue("--refresh", true));
        assertEquals("2m", parseResult.matchedOptionValue("--refresh-timeout", null));
    }

    @Test
    void unwrapsSimpleExceptionFromExecutionException() {
        var cause = new FcliSimpleException("server busy");
        var ex = AviatorSSCCorrelateSastDastCommand.toCorrelationException(new ExecutionException(cause));
        assertSame(cause, ex);
    }

    @Test
    void wrapsOtherCausesAsSimpleException() {
        var ex = AviatorSSCCorrelateSastDastCommand.toCorrelationException(
            new ExecutionException(new IllegalStateException("boom")));
        assertEquals("boom", ex.getMessage());
        assertFalse(ex.getStackTraceString().contains("IllegalStateException"));
    }
}