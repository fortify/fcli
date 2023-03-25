package com.fortify.cli.tool.fod_uploader.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.tool.common.cli.cmd.AbstractToolUninstallCommand;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Uninstall.CMD_NAME)
public class ToolFoDUploaderUninstallCommand extends AbstractToolUninstallCommand {
    @Getter @Mixin private OutputHelperMixins.Uninstall outputHelper;
    @Getter private String toolName = ToolFoDUploaderCommands.TOOL_NAME;
}
