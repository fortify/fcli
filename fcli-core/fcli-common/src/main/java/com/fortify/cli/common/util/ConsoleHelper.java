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
    
    public static final int getTerminalWidthOrDefault() {
        return getTerminalWidth()!=null ? getTerminalWidth() : 80;
    }
    
    private static final Integer determineTerminalWidth() {
        var result = getJAnsiTerminalWidth();
        if ( result==null ) {
            result = getPicocliTerminalWidth();
        }
        return result;
    }

    private static final Integer getJAnsiTerminalWidth() {
        try {
            return (Integer)Class.forName("org.fusesource.jansi.AnsiConsole")
                    .getMethod("getTerminalWidth").invoke(null);
        } catch ( Exception e ) {
            LOG.debug("Unable to determine terminal width from JANSI AnsiPrintStream: {}", e.getMessage());
        }
        return null;
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
}
