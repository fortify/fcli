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
package com.fortify.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fortify.cli.app._main.cli.cmd.FCLIRootCommands;
import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Verifies that the help of every non-deprecated, runnable Fortify Aviator command contains
 * usable examples: each example must parse successfully against the actual command definition,
 * must fit the help output width (so that it doesn't get wrapped), and every required option
 * must be mentioned in the command description.
 */
public class AviatorHelpExamplesTest {
    // Picocli wraps help text at 80 columns; wrapped examples lose their indentation
    private static final int MAX_LINE_LENGTH = 79;

    @Test
    public void testAviatorCommandHelpExamples() {
        var root = new CommandLine(FCLIRootCommands.class);
        var failures = new ArrayList<String>();
        var aviator = root.getSubcommands().get("aviator");
        Assertions.assertNotNull(aviator, "aviator command not found");
        leafCommands(aviator).distinct().forEach(cl -> checkCommand(root, cl, failures));
        Assertions.assertTrue(failures.isEmpty(), "Help example problems:\n" + String.join("\n", failures));
    }

    private static Stream<CommandLine> leafCommands(CommandLine cl) {
        var subs = cl.getSubcommands();
        return subs.isEmpty() ? Stream.of(cl) : subs.values().stream().flatMap(AviatorHelpExamplesTest::leafCommands);
    }

    private void checkCommand(CommandLine root, CommandLine cl, List<String> failures) {
        var spec = cl.getCommandSpec();
        var name = spec.qualifiedName(" ");
        if ( isDeprecated(spec) ) { return; }
        var description = new CommandSpecMessageResolver(spec).getMessageString("usage.description").replace("%n", "\n");
        var lines = Arrays.asList(description.split("\n")).stream().map(String::stripTrailing).collect(Collectors.toList());
        var examplesIdx = lines.indexOf("Examples:");
        if ( examplesIdx < 0 ) {
            failures.add(name + ": no 'Examples:' section in description");
            return;
        }
        var exampleLines = exampleSection(lines.subList(examplesIdx + 1, lines.size()));
        exampleLines.stream().filter(l -> l.length() > MAX_LINE_LENGTH).forEach(
                l -> failures.add(name + ": example line exceeds " + MAX_LINE_LENGTH + " characters: " + l.trim()));
        var commands = exampleCommands(exampleLines);
        if ( commands.stream().noneMatch(c -> c.startsWith(name + " ") || c.equals(name)) ) {
            failures.add(name + ": no example found for this command");
        }
        commands.forEach(c -> checkExampleCommand(root, name, c, failures));
        checkRequiredOptionsDocumented(spec, name, description, failures);
    }

    // The examples section ends at the first non-blank line that isn't indented, like a trailing NOTE paragraph
    private static List<String> exampleSection(List<String> lines) {
        var result = new ArrayList<String>();
        for ( var line : lines ) {
            if ( !line.isBlank() && !line.startsWith(" ") ) { break; }
            result.add(line);
        }
        return result;
    }

    // Commands start with 'fcli' on a line indented by 3 spaces; commands that don't fit on a single
    // line continue on following lines indented by 5 spaces, which are joined here before parsing.
    private static List<String> exampleCommands(List<String> exampleLines) {
        var commands = new ArrayList<String>();
        for ( var line : exampleLines ) {
            if ( line.startsWith("   fcli ") ) { commands.add(line.trim()); }
            else if ( line.startsWith("     ") && !commands.isEmpty() && line.trim().startsWith("-") ) {
                commands.set(commands.size()-1, commands.get(commands.size()-1) + " " + line.trim());
            }
        }
        return commands;
    }

    private static boolean isDeprecated(CommandSpec spec) {
        return String.join(" ", spec.usageMessage().header()).startsWith("(DEPRECATED)");
    }

    private void checkExampleCommand(CommandLine root, String name, String command, List<String> failures) {
        var args = tokenize(command);
        try {
            // Parsing validates option names, required options, mutually exclusive options, and value conversion
            root.parseArgs(args.subList(1, args.size()).toArray(String[]::new));
        } catch ( Exception e ) {
            failures.add(name + ": example fails to parse: " + command + " -> " + e.getMessage());
        }
    }

    // Options that are part of an argument group (for example mutually exclusive options) are not
    // unconditionally required, so these are not checked.
    private void checkRequiredOptionsDocumented(CommandSpec spec, String name, String description, List<String> failures) {
        spec.options().stream().filter(o -> o.required() && o.group() == null).forEach(o -> {
            if ( Stream.of(o.names()).noneMatch(n -> n.startsWith("--") && description.contains(n)) ) {
                failures.add(name + ": required option " + o.longestName() + " isn't mentioned in the description");
            }
        });
    }

    // Minimal shell-like tokenizer, supporting double-quoted values containing whitespace
    private static List<String> tokenize(String command) {
        var tokens = new ArrayList<String>();
        var current = new StringBuilder();
        var inQuotes = false;
        for ( char c : command.toCharArray() ) {
            if ( c == '"' ) { inQuotes = !inQuotes; }
            else if ( Character.isWhitespace(c) && !inQuotes ) {
                if ( current.length() > 0 ) { tokens.add(current.toString()); current.setLength(0); }
            } else { current.append(c); }
        }
        if ( current.length() > 0 ) { tokens.add(current.toString()); }
        return tokens;
    }
}
