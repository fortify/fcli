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
package com.fortify.cli.tool.sc_client.cli.cmd;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool._common.helper.ToolInstaller;
import com.fortify.cli.tool._common.helper.ToolInstaller.BinScriptType;
import com.fortify.cli.tool._common.helper.ToolInstaller.ToolInstallationResult;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class ToolSCClientInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolSCClientCommands.TOOL_NAME;
    @Option(names= {"-t", "--client-auth-token"}) private String clientAuthToken; 
    
    @Override
    protected String getDefaultArtifactType() {
        return "java";
    }
    
    @Override @SneakyThrows
    protected void postInstall(ToolInstaller installer, ToolInstallationResult installationResult) {
        updateClientAuthToken(installer.getTargetPath());
        installer.installGlobalBinScript(BinScriptType.bash, "scancentral", "bin/scancentral");
        installer.installGlobalBinScript(BinScriptType.bat, "scancentral.bat", "bin/scancentral.bat");
        installer.installGlobalBinScript(BinScriptType.bash, "pwtool", "bin/pwtool");
        installer.installGlobalBinScript(BinScriptType.bat, "pwtool.bat", "bin/pwtool.bat");
    }
    
    private void updateClientAuthToken(Path installPath) throws IOException {
        if ( StringUtils.isNotBlank(clientAuthToken) ) {
            Path clientPropertiesPath = installPath.resolve("Core/config/client.properties");
            Files.writeString(clientPropertiesPath, 
                    String.format("client_auth_token=%s", clientAuthToken), 
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
