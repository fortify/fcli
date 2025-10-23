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
    
    static {
        LOG.debug("ConsoleHelper initializing");
        // Disable JLine logging to avoid polluting application output
        java.util.logging.Logger.getLogger("org.jline").setLevel(java.util.logging.Level.OFF);
        // Not sure whether to set this at native image build time, run time, or whether this
        // even works at all. There is a native-image provider listed here:
        // https://github.com/jline/jline3/blob/947a2b8026bf29c3242ec57b160657f34f0c0c25/jansi-core/src/main/java/org/jline/jansi/AnsiConsole.java#L138
        // This is probably coming from this PR on the original JAnsi repository:
        // https://github.com/fusesource/jansi/pull/270
        // But I don't see other code from that PR in JLine Jansi, like the NativeImageFeature class
        // System.setProperty("jansi.providers", "native-image");
    }
    
    public static final boolean hasTerminal() {
        return System.console()!=null && !"true".equals(System.getProperty("fcli.no-terminal"));
    }
    
    private static final Integer determineTerminalWidth() {
        if ( !hasTerminal() ) {
            LOG.debug("No terminal detected, returning null for unlimited terminal width");
            return null;
        }
        var result = getJAnsiTerminalWidth();
        LOG.debug("Terminal width from JAnsi: {}", result);
        if ( result==null ) {
            result = getPicocliTerminalWidth();
            LOG.debug("Terminal width from Picocli: {}", result);
        }
        return result;
    }

    private static final Integer getJAnsiTerminalWidth() {
        var result = (Integer)invokeAnsiConsoleMethod("getTerminalWidth");
        if ( result == null ) {
            LOG.debug("Unable to determine terminal width from JAnsi");
        }
        return result;
    }
    
    private static final Integer getPicocliTerminalWidth() {
        try {
            CommandSpec spec = new CommandLine(DummyCommand.class).getCommandSpec();
            spec.usageMessage().autoWidth(true); // use terminal width
            return spec.usageMessage().width(); // obtain the terminal width
        } catch ( Exception e ) {
            LOG.debug("Unable to determine terminal width from picocli: {}", e.getMessage());
        }
        return null;
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
     * Invoke a static method on org.jline.jansi.AnsiConsole reflectively, only if JAnsi isn't disabled.
     * @param methodName The static method name to invoke
     * @return Result of the invocation, or null if disabled/unavailable/error
     */
    private static Object invokeAnsiConsoleMethod(String methodName) {
        if ( JAnsiConfig.JANSI_DISABLE ) {
            LOG.debug("JAnsi disabled by system property 'jansi.disable', not invoking {}", methodName);
            return null;
        }
        try {
            LOG.debug("`Invoking JAnsi method {}`", methodName);
            var clazz = Class.forName("org.jline.jansi.AnsiConsole");
            var method = clazz.getMethod(methodName);
            return method.invoke(null);
        } catch ( Throwable t ) {
            LOG.debug("Unable to invoke JAnsi method {}: {}", methodName, t.getMessage());
            return null;
        }
    }
}
