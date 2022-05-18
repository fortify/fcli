package com.fortify.cli.tools.picocli.command;

import com.fortify.cli.tools.picocli.command.entity.SampleEightball.EightballCommands;
import com.fortify.cli.tools.picocli.command.entity.ScanCentralClient.ScanCentralClientCommands;
import com.fortify.cli.tools.picocli.command.entity.SourceAndLibScanner.SourceAndLibScannerCommands;
import com.fortify.cli.tools.picocli.command.entity.VulnerabilityExporter.VulnerabilityExporterCommands;
import picocli.CommandLine.Command;

@Command(
        name="tool",
        description = "Commands for managing other useful tools and utilities.",
        subcommands = {
                EightballCommands.class,
                ScanCentralClientCommands.class,
                SourceAndLibScannerCommands.class,
                VulnerabilityExporterCommands.class
        }
)
public class ToolCommands {
}
