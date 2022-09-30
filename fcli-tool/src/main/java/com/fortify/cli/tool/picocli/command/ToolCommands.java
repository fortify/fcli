package com.fortify.cli.tool.picocli.command;

import com.fortify.cli.tool.picocli.command.entity.SampleEightball.EightballCommands;
import com.fortify.cli.tool.picocli.command.entity.ScanCentralClient.ScanCentralClientCommands;
import com.fortify.cli.tool.picocli.command.entity.SourceAndLibScanner.SourceAndLibScannerCommands;
import com.fortify.cli.tool.picocli.command.entity.VulnerabilityExporter.VulnerabilityExporterCommands;

import picocli.CommandLine.Command;

@Command(
        name="tool",
        description = "Commands for managing other useful tools and utilities.",
        resourceBundle = "com.fortify.cli.tool.i18n.ToolMessages",
        hidden = true,
        subcommands = {
                EightballCommands.class,
                ScanCentralClientCommands.class,
                SourceAndLibScannerCommands.class,
                VulnerabilityExporterCommands.class
        }
)
public class ToolCommands {
}
