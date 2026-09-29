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
import com.fortify.cli.tool._common.cli.cmd.AbstractToolRunCommand;
import com.fortify.cli.tool._common.helper.Tool;

import picocli.CommandLine.Command;

/**
 * Run command for the Fortify Agentic Analyzer tool.
 * 
 * @author Sangamesh Vijaykumar
 */
@Command(name = "run")
public class ToolFortifyAgenticAnalyzerRunCommand extends AbstractToolRunCommand {

    @Override
    protected Tool getTool() {
        return Tool.FORTIFY_AGENTIC_ANALYZER;
    }

}
