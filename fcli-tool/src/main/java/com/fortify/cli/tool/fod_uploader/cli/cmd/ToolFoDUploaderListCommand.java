package com.fortify.cli.tool.fod_uploader.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.tool.common.cli.cmd.AbstractToolListCommand;

import lombok.Getter;
import picocli.CommandLine.Command;

@Command(name = BasicOutputHelperMixins.List.CMD_NAME)
public class ToolFoDUploaderListCommand extends AbstractToolListCommand {
    @Getter private String toolName = ToolFoDUploaderCommands.TOOL_NAME;
}
