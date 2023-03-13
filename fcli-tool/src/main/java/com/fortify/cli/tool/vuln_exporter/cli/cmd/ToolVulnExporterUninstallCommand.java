package com.fortify.cli.tool.vuln_exporter.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.tool.common.cli.cmd.AbstractToolUninstallCommand;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = BasicOutputHelperMixins.Uninstall.CMD_NAME)
public class ToolVulnExporterUninstallCommand extends AbstractToolUninstallCommand {
    @Getter @Mixin private BasicOutputHelperMixins.Uninstall outputHelper;
    @Getter private String toolName = ToolVulnExporterCommands.TOOL_NAME;
}
