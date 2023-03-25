package com.fortify.cli.tool.sc_client.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.tool.common.cli.cmd.AbstractToolListCommand;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.List.CMD_NAME)
public class ToolSCClientListCommand extends AbstractToolListCommand {
    @Getter @Mixin private OutputHelperMixins.List outputHelper;
    @Getter private String toolName = ToolSCClientCommands.TOOL_NAME;
}
