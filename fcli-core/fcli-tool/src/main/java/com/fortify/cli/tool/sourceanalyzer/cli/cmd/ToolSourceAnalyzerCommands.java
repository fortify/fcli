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
package com.fortify.cli.tool.sourceanalyzer.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

/**
 * Container command for all 'fcli tool sourceanalyzer' subcommands.
 * 
 * @author Sangamesh Vijaykumar
 */
@Command(
        name = ToolSourceAnalyzerCommands.TOOL_NAME,
        subcommands = {
                ToolSourceAnalyzerListCommand.class,
                ToolSourceAnalyzerGetCommand.class,
                ToolSourceAnalyzerRegisterCommand.class,
                ToolSourceAnalyzerRunCommand.class,
                ToolSourceAnalyzerUpdateRulePacksCommand.class                
        }

)
public class ToolSourceAnalyzerCommands extends AbstractContainerCommand {
    static final String TOOL_NAME = "sourceanalyzer";
    static final String[] TOOL_ENV_VAR_PREFIXES = {"SOURCEANALYZER"};

}