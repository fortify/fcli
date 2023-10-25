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
package com.fortify.cli.tool.vuln_exporter.cli.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolInstallCommand;
import com.fortify.cli.tool._common.helper.ToolHelper;
import com.fortify.cli.tool._common.helper.ToolVersionInstallDescriptor;
import com.fortify.cli.tool._common.util.FileUtils;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Install.CMD_NAME)
public class ToolVulnExporterInstallCommand extends AbstractToolInstallCommand {
    @Getter @Mixin private OutputHelperMixins.Install outputHelper;
    @Getter private String toolName = ToolVulnExporterCommands.TOOL_NAME;
    
    @Override
    protected InstallType getInstallType() {
        return InstallType.EXTRACT_ZIP;
    }
    
    @Override
    protected void postInstall(ToolVersionInstallDescriptor descriptor) throws IOException {
        Path binPath = descriptor.getBinPath();
        Files.createDirectories(binPath);
        FileUtils.copyResourceToDir(ToolHelper.getResourceFile(getToolName(), "extra-files/bin/FortifyVulnerabilityExporter"), binPath);
        FileUtils.copyResourceToDir(ToolHelper.getResourceFile(getToolName(), "extra-files/bin/FortifyVulnerabilityExporter.bat"), binPath);
    }
}
