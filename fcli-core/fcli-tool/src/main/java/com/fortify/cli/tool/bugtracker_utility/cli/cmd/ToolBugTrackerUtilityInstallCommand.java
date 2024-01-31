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
import com.fortify.cli.tool._common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool._common.helper.ToolInstaller;
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
    protected void postInstall(ToolInstaller installer, ToolInstallationResult installationResult) {
        var targetPath = installer.getTargetPath();
        renameJar(targetPath);
        installer.installJavaBinScripts("FortifyBugTrackerUtility", "FortifyBugTrackerUtility.jar");
    }

    private void renameJar(Path targetPath) throws IOException {
        var jarFiles = Files.find(targetPath, 1, 
                (p,a)->p.toFile().getName().matches("FortifyBugTrackerUtility.*\\.jar"))
            .toList();
        if ( jarFiles.size()!=1 ) {
            throw new IllegalStateException("Unexpected number of files matching FortifyBugTrackerUtility*.jar: "+jarFiles.size());
        }
        Files.move(jarFiles.get(0), targetPath.resolve("FortifyBugTrackerUtility.jar"));
    }
}
