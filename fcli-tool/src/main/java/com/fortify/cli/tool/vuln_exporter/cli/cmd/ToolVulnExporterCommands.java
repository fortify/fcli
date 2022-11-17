package com.fortify.cli.tool.vuln_exporter.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = ToolVulnExporterCommands.TOOL_NAME,
        aliases = {"fortify-vulnerability-exporter"},
        subcommands = {
                ToolVulnExporterInstallCommand.class,
                ToolVulnExporterListCommand.class
        }

)
public class ToolVulnExporterCommands {
    static final String TOOL_NAME = "vuln-exporter";
}
