package com.fortify.cli.tool.fod_uploader.cli.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.tool.common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool.common.helper.ToolHelper;
import com.fortify.cli.tool.common.helper.ToolVersionInstallDescriptor;
import com.fortify.cli.tool.common.util.FileUtils;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = BasicOutputHelperMixins.Install.CMD_NAME)
public class ToolFoDUploaderInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private BasicOutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolFoDUploaderCommands.TOOL_NAME;
    
    @Override
    protected InstallType getInstallType() {
        return InstallType.COPY;
    }
    
    @Override
    protected void postInstall(ToolVersionInstallDescriptor descriptor) throws IOException {
        Path binPath = descriptor.getBinPath();
        Files.createDirectories(binPath);
        FileUtils.copyResourceToDir(ToolHelper.getResourceFile(getToolName(), "extra-files/bin/FoDUpload"), binPath);
        FileUtils.copyResourceToDir(ToolHelper.getResourceFile(getToolName(), "extra-files/bin/FoDUpload.bat"), binPath);
    }
}
