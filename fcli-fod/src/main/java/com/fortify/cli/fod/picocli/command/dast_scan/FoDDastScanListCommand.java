package com.fortify.cli.fod.picocli.command.dast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "list",
        description = "List all DAST scans on FoD."
)
public class FoDDastScanListCommand  extends DummyCommand {
}
