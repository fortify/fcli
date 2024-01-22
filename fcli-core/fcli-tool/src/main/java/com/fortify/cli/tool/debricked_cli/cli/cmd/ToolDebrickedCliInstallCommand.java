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
package com.fortify.cli.tool.debricked_cli.cli.cmd;

import java.nio.file.Path;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool._common.helper.ToolInstaller.ToolInstallationResult;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class ToolDebrickedCliInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolDebrickedCliCommands.TOOL_NAME;

    @Override
    protected String getDefaultArtifactType() {
        return "";
    }
    
    @Override @SneakyThrows
    protected void postInstall(ToolInstallationResult installationResult) {
        var installationDescriptor = installationResult.getInstallationDescriptor();
        Path installPath = installationDescriptor.getInstallPath();
        Path binPath = installationDescriptor.getBinPath();
        FileUtils.moveFiles(installPath, binPath, "debricked(\\.exe)?");
    }
}
