package com.fortify.cli.fod.command.entity.sast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "update",
        description = "Update an Application Release's SAST scan settings on FoD."
)
public class FODSASTScanUpdateCommand extends DummyCommand {
}
