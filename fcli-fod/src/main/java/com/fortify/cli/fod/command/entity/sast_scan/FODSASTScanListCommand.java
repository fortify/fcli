package com.fortify.cli.fod.command.entity.sast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "list",
        description = "List all SAST scans on FoD."
)
public class FODSASTScanListCommand  extends DummyCommand {
}
