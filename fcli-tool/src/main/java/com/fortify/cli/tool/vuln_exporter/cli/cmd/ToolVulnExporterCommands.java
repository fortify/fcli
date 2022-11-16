package com.fortify.cli.tool.vuln_exporter.cli.cmd;

import picocli.CommandLine.Command;

@Command(
        name = "vuln-exporter",
        aliases = {"fortify-vulnerability-exporter"},
        subcommands = {
                ToolVulnExporterInstallCommand.class
        }

)
public class ToolVulnExporterCommands {}
