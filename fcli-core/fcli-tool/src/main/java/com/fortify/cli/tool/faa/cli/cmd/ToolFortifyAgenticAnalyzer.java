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
package com.fortify.cli.tool.faa.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

/**
 * Container command for all 'fcli tool fortify-agentic-analyzer' subcommands.
 * 
 * @author Sangamesh Vijaykumar
*/
@Command(
    name = ToolFortifyAgenticAnalyzer.TOOL_NAME,
    aliases = {"faa"},
    subcommands = {
        ToolFortifyAgenticAnalyzerListCommand.class,
        ToolFortifyAgenticAnalyzerGetCommand.class,
        ToolFortifyAgenticAnalyzerRegisterCommand.class,
        ToolFortifyAgenticAnalyzerRunCommand.class
    } 
)
public class ToolFortifyAgenticAnalyzer extends AbstractContainerCommand {
    static final String TOOL_NAME = "fortify-agentic-analyzer";
    static final String[] TOOL_ENV_VAR_PREFIXES = {"FORTIFY_AGENTIC_ANALYZER"};

}
