package com.fortify.cli.tool.vuln_exporter.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = ToolVulnExporterCommands.TOOL_NAME,
        aliases = {"fortify-vulnerability-exporter"},
        subcommands = {
                ToolVulnExporterInstallCommand.class,
                ToolVulnExporterListCommand.class,
                ToolVulnExporterUninstallCommand.class
        }

)
public class ToolVulnExporterCommands extends AbstractFortifyCLICommand {
    static final String TOOL_NAME = "vuln-exporter";
}
