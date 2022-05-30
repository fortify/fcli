package com.fortify.cli.fod.picocli.command.sast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "update",
        description = "Update an Application Release's SAST scan settings on FoD."
)
public class FoDSastScanUpdateCommand extends DummyCommand {
}
