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

import java.nio.file.Files;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool._common.helper.ToolInstaller.ToolInstallationResult;

import lombok.Getter;
import lombok.SneakyThrows;
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
    
    @Override @SneakyThrows
    protected void postInstall(ToolInstallationResult installationResult) {
        var installationDescriptor = installationResult.getInstallationDescriptor();
        copyBinResource(installationResult, "extra-files/bin/FortifyBugTrackerUtility");
        copyBinResource(installationResult, "extra-files/bin/FortifyBugTrackerUtility.bat");
        copyGlobalBinResource(installationResult, "extra-files/global_bin/FortifyBugTrackerUtility");
        copyGlobalBinResource(installationResult, "extra-files/global_bin/FortifyBugTrackerUtility.bat");
        
        var jarFiles = Files.find(installationDescriptor.getInstallPath(), 1, 
                (p,a)->p.toFile().getName().matches("FortifyBugTrackerUtility.*\\.jar"))
            .toList();
        if ( jarFiles.size()!=1 ) {
            throw new IllegalStateException("Unexpected number of files matching FortifyBugTrackerUtility*.jar: "+jarFiles.size());
        }
        jarFiles.get(0).toFile().renameTo(installationDescriptor.getInstallPath().resolve("FortifyBugTrackerUtility.jar").toFile());
    }
}
