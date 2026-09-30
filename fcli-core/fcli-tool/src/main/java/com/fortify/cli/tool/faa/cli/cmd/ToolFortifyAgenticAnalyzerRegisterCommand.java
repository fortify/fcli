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

import java.io.File;

import com.fortify.cli.tool._common.cli.cmd.AbstractToolRegisterCommand;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolVersionDetector;

import picocli.CommandLine.Command;

/**
 * Register command for the Fortify Agentic Analyzer tool.
 * 
 * @author Sangamesh Vijaykumar
 */
@Command(name = "register")
public class ToolFortifyAgenticAnalyzerRegisterCommand extends AbstractToolRegisterCommand{

    @Override
    protected Tool getTool() {
        return Tool.FORTIFY_AGENTIC_ANALYZER;
    }

    @Override
    protected String detectVersion(File toolBinary, File installDir) {
        // Execute fortifyaa to detect its version
        String output = ToolVersionDetector.tryExecute(toolBinary, "");
        if (output != null) {
            String version = ToolVersionDetector.extractVersionFromOutput(output);
            if (version != null) {
                return version;
            }
        }
        
        return "unknown";
    }

}
