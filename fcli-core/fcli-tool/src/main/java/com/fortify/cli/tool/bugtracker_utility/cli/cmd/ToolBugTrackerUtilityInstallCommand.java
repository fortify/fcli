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
package com.fortify.cli.tool.bugtracker_utility.cli.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool._common.helper.ToolDefinitionArtifactDescriptor;
import com.fortify.cli.tool._common.helper.ToolDefinitionVersionDescriptor;
import com.fortify.cli.tool._common.helper.ToolHelper;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class ToolBugTrackerUtilityInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolBugTrackerUtilityCommands.TOOL_NAME;
    
    @Override
    protected String getDefaultArtifactType() {
        return "java";
    }
    
    @Override
    protected void postInstall(ToolDefinitionVersionDescriptor versionDescriptor, ToolDefinitionArtifactDescriptor artifactDescriptor, ToolInstallationDescriptor installationDescriptor) throws IOException {
        Path binPath = installationDescriptor.getBinPath();
        FileUtils.copyResourceToDir(ToolHelper.getResourceFile(getToolName(), "extra-files/bin/FortifyBugTrackerUtility"), binPath);
        FileUtils.copyResourceToDir(ToolHelper.getResourceFile(getToolName(), "extra-files/bin/FortifyBugTrackerUtility.bat"), binPath);
        
        var jarFiles = Files.find(installationDescriptor.getInstallPath(), 1, 
                (p,a)->p.toFile().getName().matches("FortifyBugTrackerUtility.*\\.jar"))
            .toList();
        if ( jarFiles.size()!=1 ) {
            throw new IllegalStateException("Unexpected number of files matching FortifyBugTrackerUtility*.jar: "+jarFiles.size());
        }
        jarFiles.get(0).toFile().renameTo(installationDescriptor.getInstallPath().resolve("FortifyBugTrackerUtility.jar").toFile());
    }
}