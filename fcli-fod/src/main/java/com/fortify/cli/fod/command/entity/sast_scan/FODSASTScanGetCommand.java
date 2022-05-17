package com.fortify.cli.fod.command.entity.sast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "get",
        description = "Get a SAST scan from FoD."
)
public class FODSASTScanGetCommand  extends DummyCommand {
}
