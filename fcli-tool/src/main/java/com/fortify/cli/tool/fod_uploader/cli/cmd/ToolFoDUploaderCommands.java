package com.fortify.cli.tool.fod_uploader.cli.cmd;

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
public class ToolFoDUploaderCommands {
    static final String TOOL_NAME = "fod-uploader";
}
