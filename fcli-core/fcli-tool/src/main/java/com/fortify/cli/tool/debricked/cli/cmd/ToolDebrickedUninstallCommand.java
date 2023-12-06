/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.tool.debricked.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolUninstallCommand;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Uninstall.CMD_NAME)
public class ToolDebrickedUninstallCommand extends AbstractToolUninstallCommand {
    @Getter @Mixin private OutputHelperMixins.Uninstall outputHelper;
    @Getter private String toolName = ToolDebrickedCommands.TOOL_NAME;
    @Getter @Option(names={"-a", "--cpu-architecture"}, required = true, descriptionKey="fcli.tool.debricked.uninstall.cpuArchitecture", defaultValue = "x86_64") 
    private String _cpuArchitecture;

    @Override
    protected String getCpuArchitecture() {
        return _cpuArchitecture;
    }
}
