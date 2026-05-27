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

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.fcli;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepRecordsForEach.IActionStepForEachProcessor;
import com.fortify.cli.common.cli.util.CommandSpecDescriptor;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;
import com.fortify.cli.common.spel.query.QueryExpressionTypeConverter;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * SpEL functions for listing and querying fcli commands from within action YAML files.
 * Available via the {@code #fcli} SpEL variable.
 */
@Reflectable @NoArgsConstructor
@SpelFunctionPrefix("fcli.")
public final class FcliCommandsSpelFunctions {
    public static final FcliCommandsSpelFunctions INSTANCE = new FcliCommandsSpelFunctions();
    private static final QueryExpressionTypeConverter QUERY_CONVERTER = new QueryExpressionTypeConverter();

    @SpelFunction(cat = fcli, desc = """
        The return value of this function can be passed to a `records.for-each::from` instruction to \\
        iterate over all available fcli commands. Each record produced by the processor will be an \\
        ObjectNode with the same same fields as returned by `fcli util all-commands list --json`.
        """,
        returns = "Processor for iterating over all available fcli command descriptors")
    public IActionStepForEachProcessor listCommands() {
        return IActionStepForEachProcessor.fromStream(
                CommandSpecDescriptor.rootDescriptorStream()
                        .map(CommandSpecDescriptor::getCommandSpecNode));
    }

    @SneakyThrows
    @SpelFunction(cat = fcli, desc = """
        The return value of this function can be passed to a `records.for-each::from` instruction to \\
        iterate over fcli commands matching the given SpEL query expression, similar to the `--query` \\
        option of `fcli util all-commands list`. Each record produced by the processor will be an \\
        ObjectNode with the same same fields as returned by `fcli util all-commands list --json`.
        """,
        returns = "Processor for iterating over matching fcli command descriptors")
    public IActionStepForEachProcessor listCommands(
            @SpelFunctionParam(name = "query", desc = "SpEL filter expression, e.g. \"module=='ssc' && !hidden\"") String query) {
        var queryExpression = QUERY_CONVERTER.convert(query);
        return IActionStepForEachProcessor.fromStream(
                CommandSpecDescriptor.rootDescriptorStream()
                        .filter(d -> d.matches(queryExpression))
                        .map(CommandSpecDescriptor::getCommandSpecNode));
    }

    @SpelFunction(cat = fcli, desc = "Returns the command spec node for the given fully-qualified command name, or null if not found. The node contains fields: command, module, entity, action, hidden, runnable, usageHeader, usageDescription, aliases, options, metadata.",
            returns = "ObjectNode with command spec fields, or null if command not found")
    public ObjectNode getCommandSpec(
            @SpelFunctionParam(name = "command", desc = "fully-qualified command name, e.g. \"fcli ssc app list\"") String command) {
        var desc = CommandSpecDescriptor.of(command);
        return desc == null ? null : desc.getCommandSpecNode();
    }

    @SpelFunction(cat = fcli, desc = "Returns the command args node for the given fully-qualified command name, or null if not found. The node contains a 'parameters' array and an 'optionGroups' array, each option having: title, names, primaryName, valueFormat, description, required, datatype, secret, multiselect, allowedValues.",
            returns = "ObjectNode with 'parameters' and 'optionGroups' fields, or null if command not found")
    public ObjectNode getCommandArgs(
            @SpelFunctionParam(name = "command", desc = "fully-qualified command name, e.g. \"fcli ssc app list\"") String command) {
        var desc = CommandSpecDescriptor.of(command);
        return desc == null ? null : desc.getCommandArgsNode();
    }
}
