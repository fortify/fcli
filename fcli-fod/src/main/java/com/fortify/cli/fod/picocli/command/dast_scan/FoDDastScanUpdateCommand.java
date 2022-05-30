package com.fortify.cli.fod.picocli.command.dast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "update",
        description = "Update an Application Release's DAST scan settings on FoD."
)
public class FoDDastScanUpdateCommand  extends DummyCommand {
}
