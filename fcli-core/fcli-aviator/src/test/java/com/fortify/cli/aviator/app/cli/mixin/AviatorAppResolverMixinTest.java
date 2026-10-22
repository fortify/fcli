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
package com.fortify.cli.aviator.app.cli.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParameterException;

class AviatorAppResolverMixinTest {
    private static final String MESSAGES = "com/fortify/cli/aviator/i18n/AviatorMessages";

    @Command(name = "test", resourceBundle = MESSAGES)
    private static final class TestCommand {
        @Mixin private AviatorAppResolverMixin.NameOrIdParameter appResolver;
    }

    private static CommandLine commandLine() {
        return new CommandLine(new TestCommand());
    }

    private static String parse(String... args) {
        var commandLine = commandLine();
        commandLine.parseArgs(args);
        return ((TestCommand) commandLine.getCommand()).appResolver.getAppNameOrId();
    }

    @Test
    void positionalParameterIsPassedThroughForNames() {
        assertEquals("MyApp", parse("MyApp"));
    }

    @Test
    void positionalParameterIsPassedThroughForIds() {
        assertEquals("123", parse("123"));
    }

    @Test
    void positionalParameterIsRequired() {
        assertThrows(ParameterException.class, () -> parse());
    }

    @Test
    void helpTextDescribesNameOrIdRules() {
        var help = commandLine().getUsageMessage().replaceAll("\\s+", " ");
        assertTrue(help.contains("<app-name-or-id>"), help);
        assertTrue(help.contains("Numeric values are always interpreted as an application id"), help);
        assertTrue(help.contains("purely numeric name can only be specified by its id"), help);
    }

    @Test
    void messageBundleDefinesDescriptionKey() {
        assertTrue(ResourceBundle.getBundle(MESSAGES).containsKey("fcli.aviator.app.app-name-or-id"));
    }
    @Command(name = "test-option", resourceBundle = MESSAGES)
    private static final class OptionCommand {
        @Mixin private AviatorAppResolverMixin.NameOnlyOption appResolver;
    }

    private static String parseOption(String... args) {
        var commandLine = new CommandLine(new OptionCommand());
        commandLine.parseArgs(args);
        return ((OptionCommand) commandLine.getCommand()).appResolver.getAppName();
    }

    @Test
    void appOptionIsNameOnlyAndPassedThroughUnchanged() {
        assertEquals("MyApp", parseOption("--app", "MyApp"));
        assertEquals("123", parseOption("--app", "123"));
    }

    @Test
    void appOptionIsOptional() {
        assertNull(parseOption());
    }
}
