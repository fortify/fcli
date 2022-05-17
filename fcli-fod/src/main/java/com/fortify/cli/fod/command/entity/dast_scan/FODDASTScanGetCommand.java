package com.fortify.cli.fod.command.entity.dast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "get",
        description = "Get a DAST scan from FoD."
)
public class FODDASTScanGetCommand  extends DummyCommand {
}
