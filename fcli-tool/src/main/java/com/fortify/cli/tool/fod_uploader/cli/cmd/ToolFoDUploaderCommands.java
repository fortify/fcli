package com.fortify.cli.tool.fod_uploader.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = ToolFoDUploaderCommands.TOOL_NAME,
        aliases = {"fodupload"},
        subcommands = {
                ToolFoDUploaderInstallCommand.class,
                ToolFoDUploaderListCommand.class,
                ToolFoDUploaderUninstallCommand.class
        }

)
public class ToolFoDUploaderCommands extends AbstractFortifyCLICommand {
    static final String TOOL_NAME = "fod-uploader";
}
