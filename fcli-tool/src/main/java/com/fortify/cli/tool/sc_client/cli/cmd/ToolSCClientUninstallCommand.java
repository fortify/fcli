package com.fortify.cli.tool.sc_client.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.tool.common.cli.cmd.AbstractToolUninstallCommand;

import lombok.Getter;
import picocli.CommandLine.Command;

@Command(name = BasicOutputHelperMixins.Uninstall.CMD_NAME)
public class ToolSCClientUninstallCommand extends AbstractToolUninstallCommand {
    @Getter private String toolName = ToolSCClientCommands.TOOL_NAME;
}
