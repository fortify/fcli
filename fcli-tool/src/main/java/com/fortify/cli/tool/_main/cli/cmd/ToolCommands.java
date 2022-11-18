package com.fortify.cli.tool._main.cli.cmd;

import com.fortify.cli.tool.fod_uploader.cli.cmd.ToolFoDUploaderCommands;
import com.fortify.cli.tool.sc_client.cli.cmd.ToolSCClientCommands;
import com.fortify.cli.tool.vuln_exporter.cli.cmd.ToolVulnExporterCommands;

import picocli.CommandLine.Command;

@Command(
        name = "tool",
        resourceBundle = "com.fortify.cli.tool.i18n.ToolMessages",
        subcommands = {
            ToolFoDUploaderCommands.class,
            ToolSCClientCommands.class,
            ToolVulnExporterCommands.class
        }
)
public class ToolCommands {}
