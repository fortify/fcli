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

import java.util.List;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.PlatformHelper;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolRunCommand;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Command for running Fortify Source Analyzer. This command allows for running Fortify Source Analyzer as already installed in the user's machine, \
 * and is not limited to running versions of Fortify Source Analyzer that were installed through the 'fcli tool sourceanalyzer install' command. It is recommended to use double dashes to separate fcli options from Fortify Source Analyzer options, \
 * i.e., 'fcli tool sourceanalyzer run <fcli options> -- <Fortify Source Analyzer options>' to explicitly differentiate between fcli options and Fortify Source Analyzer options.
 * 
 * @author Sangamesh Vijaykumar
 */
@Command(name = "run")
public class ToolSourceAnalyzerRunCommand extends AbstractToolRunCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    
    @Override
    protected final Tool getTool() {
        return Tool.SOURCE_ANALYZER;
    }

    @Override
    protected List<String> getBaseCommand(ToolInstallationDescriptor descriptor) {
        var baseCmd = PlatformHelper.isWindows() ? "sourceanalyzer.exe" : "sourceanalyzer";
        return List.of(descriptor.getBinPath().resolve(baseCmd).toString());
    }
}
