package com.fortify.cli.fod.picocli.command.sast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "get",
        description = "Get a SAST scan from FoD."
)
public class FoDSastScanGetCommand  extends DummyCommand {
}
