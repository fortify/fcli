package com.fortify.cli.tool.sc_client.cli.cmd;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.tool.common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool.common.helper.ToolVersionInstallDescriptor;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = BasicOutputHelperMixins.Install.CMD_NAME)
public class ToolSCClientInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private BasicOutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolSCClientCommands.TOOL_NAME;
    @Option(names= {"-t", "--client-auth-token"}) private String clientAuthToken; 
    
    @Override
    protected InstallType getInstallType() {
        return InstallType.EXTRACT_ZIP;
    }
    
    @Override
    protected void postInstall(ToolVersionInstallDescriptor descriptor) throws IOException {
        // Updating bin permissions is handled by parent class
        updateClientAuthToken(descriptor.getInstallPath());
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
