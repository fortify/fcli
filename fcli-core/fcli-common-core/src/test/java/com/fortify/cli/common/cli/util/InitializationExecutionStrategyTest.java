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
package com.fortify.cli.common.cli.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskLevel;
import com.fortify.cli.common.log.LogMessageType;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.rest.cli.mixin.UnirestContextMixin;
import com.fortify.cli.common.rest.unirest.RemoteUrlAuthHelper;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

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

    @Command(name="dummy-positional")
    static class DummyPositionalMaskCommand extends AbstractRunnableCommand {
        static String maskedValueObservedInCall;

        @Parameters(index = "0")
        @MaskValue(sensitivity = LogSensitivityLevel.high, description = "REMOTE URL AUTH VALUE", pattern = RemoteUrlAuthHelper.URL_USERINFO_AUTH_VALUE_MASK_PATTERN, maskFullValueOnNoMatch = RemoteUrlAuthHelper.URL_USERINFO_AUTH_VALUE_MASK_FULL_ON_NO_MATCH)
        private String source;

        @Override
        public Integer call() {
            maskedValueObservedInCall = LogMaskHelper.INSTANCE.mask(LogMessageType.FCLI, source);
            return 0;
        }
    }

    @Test
    void strategyRegistersPositionalLogMaskValues() {
        var source = "https://user:positionalMaskSecret12345@example.com/path";
        DummyPositionalMaskCommand.maskedValueObservedInCall = null;
        var cmd = new CommandLine(new DummyPositionalMaskCommand());
        FcliExecutionStrategyFactory.configureCommandLine(cmd);
        LogMaskHelper.INSTANCE.setLogMaskLevel(LogMaskLevel.high);
        int exitCode = cmd.execute(source);
        assertEquals(0, exitCode);
        assertNotNull(DummyPositionalMaskCommand.maskedValueObservedInCall);
        assertFalse(DummyPositionalMaskCommand.maskedValueObservedInCall.contains("positionalMaskSecret12345"));
    }

    @Test
    void strategyDoesNotMaskPositionalRemoteUrlWithoutUserinfo() {
        var source = "https://untrusted-root.badssl.com/";
        DummyPositionalMaskCommand.maskedValueObservedInCall = null;
        var cmd = new CommandLine(new DummyPositionalMaskCommand());
        FcliExecutionStrategyFactory.configureCommandLine(cmd);
        LogMaskHelper.INSTANCE.setLogMaskLevel(LogMaskLevel.high);
        int exitCode = cmd.execute(source);
        assertEquals(0, exitCode);
        assertNotNull(DummyPositionalMaskCommand.maskedValueObservedInCall);
        assertEquals(source, DummyPositionalMaskCommand.maskedValueObservedInCall);
    }
}