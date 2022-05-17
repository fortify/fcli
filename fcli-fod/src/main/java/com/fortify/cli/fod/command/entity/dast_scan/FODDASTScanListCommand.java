package com.fortify.cli.fod.command.entity.dast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "list",
        description = "List all DAST scans on FoD."
)
public class FODDASTScanListCommand  extends DummyCommand {
}
