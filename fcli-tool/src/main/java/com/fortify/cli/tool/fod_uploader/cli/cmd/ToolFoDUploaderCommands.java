package com.fortify.cli.tool.fod_uploader.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "fod-uploader",
        aliases = {"fodupload"},
        subcommands = {
                ToolFoDUploaderInstallCommand.class
        }

)
public class ToolFoDUploaderCommands {}
