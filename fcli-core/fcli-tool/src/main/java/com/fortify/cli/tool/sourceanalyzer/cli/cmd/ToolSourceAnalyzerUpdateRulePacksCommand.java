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

import com.fortify.cli.common.util.PlatformHelper;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolRunCommand;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;

import picocli.CommandLine.Command;

/**
 * Command to update Fortify Source Analyzer rulepacks by running the
 * fortifyupdate binary from the registered installation.
 *
 * This command uses the same installation resolution logic as other
 * sourceanalyzer tool commands and simply executes the platform-specific
 * fortifyupdate executable from the installation's bin directory.
 * 
 * @author Sangamesh Vijaykumar
 */
@Command(name = "update-rules")
public class ToolSourceAnalyzerUpdateRulePacksCommand extends AbstractToolRunCommand {
    @Override
    protected final Tool getTool() {
        return Tool.SOURCE_ANALYZER;
    }

    @Override
    protected List<String> getBaseCommand(ToolInstallationDescriptor descriptor) {
        var baseCmd = PlatformHelper.isWindows() ? "fortifyupdate.cmd" : "fortifyupdate";
        return List.of(descriptor.getBinPath().resolve(baseCmd).toString());
    }
}
