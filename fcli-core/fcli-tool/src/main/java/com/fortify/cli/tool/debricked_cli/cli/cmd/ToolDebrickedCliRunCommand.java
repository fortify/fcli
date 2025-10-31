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
package com.fortify.cli.tool.debricked_cli.cli.cmd;

import java.util.List;

import com.fortify.cli.tool._common.cli.cmd.AbstractToolRunCommand;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;

import lombok.Getter;
import picocli.CommandLine.Command;

@Command(name = "run")
public class ToolDebrickedCliRunCommand extends AbstractToolRunCommand {
    @Getter private String toolName = ToolDebrickedCliCommands.TOOL_NAME;

    @Override
    protected List<String> getBaseCommand(ToolInstallationDescriptor descriptor) {
        var baseCmd = ToolPlatformHelper.isWindows() ? "debricked.exe" : "debricked";
        return List.of(descriptor.getBinPath().resolve(baseCmd).toString());
    }
}
