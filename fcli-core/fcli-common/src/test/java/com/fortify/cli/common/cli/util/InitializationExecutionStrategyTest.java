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
package com.fortify.cli.common.cli.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.rest.cli.mixin.UnirestContextMixin;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Integration-style test validating that the combined execution strategy automatically
 * performs CommandSpec and UnirestContext injections without requiring explicit initialize() calls.
 */
class InitializationExecutionStrategyTest {
    @Command(name="dummy")
    static class DummyCommand extends AbstractRunnableCommand {
        @Mixin UnirestContextMixin unirest;
        @Override public Integer call() {
            // CommandHelperMixin should have been injected prior to call
            assertNotNull(getCommandHelper().getCommandSpec(), "CommandSpec must be injected");
            assertNotNull(unirest.getUnirestContext(), "UnirestContext must be injected");
            // Verify qualified name resolution works
            assertEquals("dummy", getCommandHelper().getCommandSpec().name());
            return 0;
        }
    }

    @Test
    void strategyInjectsHelpers() {
        var cmd = new CommandLine(new DummyCommand());
        FcliExecutionStrategyFactory.configureCommandLine(cmd);
        int exitCode = cmd.execute();
        assertEquals(0, exitCode);
    }
}