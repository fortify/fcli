/**
 * Copyright 2023 Open Text.
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.action.helper.runner.ActionRunner.StepProcessingException;
import com.fortify.cli.common.output.cli.cmd.IOutputHelperSupplier;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputWriter;
import com.fortify.cli.common.util.JavaHelper;
import com.fortify.cli.common.util.OutputCollector;
import com.fortify.cli.common.util.OutputCollector.Output;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE) @Data
public final class FcliCommandExecutor {
    private final CommandLine rootCommandLine;
    private final ParseResult parseResult;
    
    public FcliCommandExecutor(CommandLine rootCommandLine, String cmd) {
        this(rootCommandLine, parse(rootCommandLine, cmd));
    }
    
    public FcliCommandExecutor(CommandLine rootCommandLine, String[] args) {
        this(rootCommandLine, parse(rootCommandLine, args));
    }
    
    public final CommandSpec getLeafCommandSpec(ParseResult parseResult) {
        var leafCommand = parseResult.subcommand();
        while (leafCommand.hasSubcommand() ) {
            leafCommand = leafCommand.subcommand();
        }
        return leafCommand.commandSpec();
    }
    
    public final <T> Optional<T> getLeafCommand(ParseResult parseResult, Class<T> type) {
        return JavaHelper.as(getLeafCommandSpec(parseResult).userObject(), type);
    }
    
    public final Output execute() {
        return OutputCollector.collectOutput(StandardCharsets.UTF_8, ()->_execute());
    }
    
    public final boolean canCollectRecords() {
        return getLeafCommand(getParseResult(), IOutputHelperSupplier.class).isPresent();
    }
    
    public final Output execute(Consumer<ObjectNode> recordConsumer, boolean suppressOutput) {
        if ( canCollectRecords() && recordConsumer!=null ) {
            StandardOutputWriter.collectRecords(recordConsumer, suppressOutput);
        }
        return execute();
    }

    private final int _execute() {
        try {
            rootCommandLine.clearExecutionResults();
            return rootCommandLine.getExecutionStrategy().execute(parseResult);
        } catch ( Exception e ) {
            throw new StepProcessingException("Fcli command threw an exception", e);
        }
    }
    
    private static final ParseResult parse(CommandLine rootCommandLine, String cmd) {
        List<String> argsList = new ArrayList<String>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(cmd);
        while (m.find()) { argsList.add(m.group(1).replace("\"", "")); }
        return parse(rootCommandLine, argsList.toArray(String[]::new));
    }
    
    private static final ParseResult parse(CommandLine rootCommandLine, String[] args) {
        return rootCommandLine.parseArgs(args);
    }
}
