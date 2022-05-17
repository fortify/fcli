package com.fortify.cli.fod.command.entity.dast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "update",
        description = "Update an Application Release's DAST scan settings on FoD."
)
public class FODDASTScanUpdateCommand  extends DummyCommand {
}
