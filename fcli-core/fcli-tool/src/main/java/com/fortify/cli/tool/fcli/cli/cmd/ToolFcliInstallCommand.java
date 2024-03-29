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
package com.fortify.cli.tool.fcli.cli.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.FileUtils;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool._common.helper.ToolInstaller;
import com.fortify.cli.tool._common.helper.ToolInstaller.BinScriptType;
import com.fortify.cli.tool._common.helper.ToolInstaller.ToolInstallationResult;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class ToolFcliInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolFcliCommands.TOOL_NAME;
    
    @Override
    protected String getDefaultArtifactType() {
        return "java";
    }
    
    @Override @SneakyThrows
    protected void postInstall(ToolInstaller installer, ToolInstallationResult installationResult) {
        installBinariesAndScripts(installer);
        installFcliCompletion(installer);
    }

    private void installFcliCompletion(ToolInstaller installer) throws IOException {
        var globalBinPath = installer.getGlobalBinPath();
        var originalFcliCompletionScript = installer.getBinPath().resolve("fcli_completion");
        if ( Files.exists(originalFcliCompletionScript) ) {
            var pw = installer.getProgressWriter();
            Path targetFcliCompletionScript = originalFcliCompletionScript;
            if ( globalBinPath!=null ) {
                targetFcliCompletionScript = globalBinPath.resolve(originalFcliCompletionScript.getFileName());
                Files.copy(originalFcliCompletionScript, targetFcliCompletionScript, StandardCopyOption.REPLACE_EXISTING);
            }
            pw.writeWarning("INFO: Run the following command to update fcli auto-completion:\n  source %s", targetFcliCompletionScript.toAbsolutePath().normalize());
        }
    }

    private void installBinariesAndScripts(ToolInstaller installer) {
        Path installPath = installer.getTargetPath();
        FileUtils.moveFiles(installPath, installer.getBinPath(), "fcli(_completion)?(\\.exe)?");
        if ( Files.exists(installPath.resolve("fcli.jar")) ) {
            installer.installJavaBinScripts("fcli", "fcli.jar");
        } else {
            installer.installGlobalBinScript(BinScriptType.bash, "fcli", "bin/fcli");
            installer.installGlobalBinScript(BinScriptType.bat, "fcli.bat", "bin/fcli.exe");
        }
    }
}
