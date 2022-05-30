package com.fortify.cli.sc_sast.picocli.command.scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine.Command;

@Command(
        name = "scan",
        description = "Commands related to scanning with Fortify ScanCentral SAST.",
        subcommands = {
                DummyCommand.class,
                SCSASTScanPrepareCommand.class,
                SCSASTScanStartCommand.class,
                SCSASTScanCancelCommand.class,
                SCSASTScanStatusCommand.class,
                SCSASTScanListCommand.class
        }
)
public class SCSASTScanCommands {
}
