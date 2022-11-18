package com.fortify.cli.tool.vuln_exporter.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.tool.common.cli.cmd.AbstractToolListCommand;

import lombok.Getter;
import picocli.CommandLine.Command;

@Command(name = BasicOutputHelperMixins.List.CMD_NAME)
public class ToolVulnExporterListCommand extends AbstractToolListCommand {
    @Getter private String toolName = ToolVulnExporterCommands.TOOL_NAME;
}
