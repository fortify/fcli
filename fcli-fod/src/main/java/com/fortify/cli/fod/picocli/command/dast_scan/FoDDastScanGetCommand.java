package com.fortify.cli.fod.picocli.command.dast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "get",
        description = "Get a DAST scan from FoD."
)
public class FoDDastScanGetCommand  extends DummyCommand {
}
