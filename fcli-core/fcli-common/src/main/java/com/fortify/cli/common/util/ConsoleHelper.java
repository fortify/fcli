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
package com.fortify.cli.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

public class ConsoleHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleHelper.class);
    @Getter(lazy=true) private static final Integer terminalWidth = determineTerminalWidth();
    
    public static final boolean hasTerminal() {
        return System.console()!=null && !"true".equals(System.getProperty("fcli.no-terminal"));
    }
    
    private static final Integer determineTerminalWidth() {
        var result = getTerminalWidthFromSystemProperty();
        if ( result==null && !hasTerminal() ) {
            LOG.debug("No terminal detected, returning null for unlimited terminal width");
            return null;
        }
        if ( result==null ) {
            result = getJAnsiTerminalWidth();
        }
        if ( result==null ) {
            result = getPicocliTerminalWidth();
        }
        return result;
    }
    
    private static final Integer getTerminalWidthFromSystemProperty() {
        Integer result = null;
        String propValue = System.getProperty("fcli.terminal.width");
        if ( propValue!=null ) {
            try {
                result = Integer.valueOf(propValue);
            } catch ( NumberFormatException nfe ) {
                LOG.warn("Invalid value for system property 'fcli.terminal.width': {}", propValue);
            }
        }
        LOG.debug("Terminal width from system property 'fcli.terminal.width': {}", result);
        return result;
    }

    private static final Integer getJAnsiTerminalWidth() {
        var result = (Integer)invokeAnsiConsoleMethod("getTerminalWidth");
        if ( result!=null && result<=0 ) { // JAnsi returns 0 if it cannot determine the terminal width
            result = null;
        }
        LOG.debug("Terminal width from JAnsi: {}", result);
        return result;
    }
    
    private static final Integer getPicocliTerminalWidth() {
        Integer result = null;
        try {
            CommandSpec spec = new CommandLine(DummyCommand.class).getCommandSpec();
            spec.usageMessage().autoWidth(true); // use terminal width
            result = spec.usageMessage().width(); // obtain the terminal width
        } catch ( Exception e ) {
            LOG.debug("Unable to determine terminal width from picocli: {}", e.getMessage());
        }
        LOG.debug("Terminal width from Picocli: {}", result);
        return result;
    }
    
    @Command(name = "dummy")
    public static final class DummyCommand {}

    /**
     * Install the JAnsi console if not disabled. Safe no-op if disabled or unavailable.
     */
    public static final void installAnsiConsole() {
        invokeAnsiConsoleMethod("systemInstall");
    }

    /**
     * Uninstall the JAnsi console if previously installed (and not disabled). Safe no-op otherwise.
     */
    public static final void uninstallAnsiConsole() {
        invokeAnsiConsoleMethod("systemUninstall");
    }

    /**
     * Invoke a static method on org.fusesource.jansi.AnsiConsole reflectively, only if JAnsi isn't disabled.
     * @param methodName The static method name to invoke
     * @return Result of the invocation, or null if disabled/unavailable/error
     */
    private static Object invokeAnsiConsoleMethod(String methodName) {
        if ( JAnsiConfig.JANSI_DISABLE ) {
            LOG.debug("JAnsi disabled by system property 'jansi.disable', not invoking {}", methodName);
            return null;
        }
        try {
            LOG.debug("Invoking JAnsi method {}", methodName);
            var clazz = Class.forName("org.fusesource.jansi.AnsiConsole");
            var method = clazz.getMethod(methodName);
            return method.invoke(null);
        } catch ( Throwable t ) {
            LOG.debug("Unable to invoke JAnsi method {}: {}", methodName, t.getMessage());
            return null;
        }
    }
}
