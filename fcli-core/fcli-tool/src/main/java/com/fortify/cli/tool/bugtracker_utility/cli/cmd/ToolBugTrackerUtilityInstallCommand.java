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
import com.fortify.cli.tool._common.helper.ToolHelper;
import com.fortify.cli.tool._common.helper.ToolVersionInstallDescriptor;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class ToolBugTrackerUtilityInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolBugTrackerUtilityCommands.TOOL_NAME;
    
    @Override
    protected void postInstall(ToolVersionInstallDescriptor descriptor) throws IOException {
        Path binPath = descriptor.getBinPath();
        Files.createDirectories(binPath);
        FileUtils.copyResourceToDir(ToolHelper.getResourceFile(getToolName(), "extra-files/bin/FortifyBugTrackerUtility"), binPath);
        FileUtils.copyResourceToDir(ToolHelper.getResourceFile(getToolName(), "extra-files/bin/FortifyBugTrackerUtility.bat"), binPath);
        
        String version = descriptor.getOriginalDownloadDescriptor().getVersion();
        String jarName = String.format("FortifyBugTrackerUtility-%s.jar", version);
        
        //we are renaming the jar to remove the version reference
        //this allows us to use pre-written bat/bash wrappers rather than having to dynamically generate those
        descriptor.getInstallPath().resolve(jarName).toFile().renameTo(
                descriptor.getInstallPath().resolve("FortifyBugTrackerUtility.jar").toFile());
        
    }
}
