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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool._common.helper.ToolDefinitionArtifactDescriptor;
import com.fortify.cli.tool._common.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class ToolDebrickedCliInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolDebrickedCliCommands.TOOL_NAME;

    
    @Override
    protected void postInstall(ToolDefinitionVersionDescriptor versionDescriptor, ToolDefinitionArtifactDescriptor artifactDescriptor, ToolInstallationDescriptor installationDescriptor) throws IOException {
        Path binPath = installationDescriptor.getBinPath();
        Files.createDirectories(binPath);
        for(File f : installationDescriptor.getInstallPath().toFile().listFiles()) {
            if(f.getAbsolutePath()!=binPath.toString()) {
                Files.move(f.toPath(), binPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    
}
