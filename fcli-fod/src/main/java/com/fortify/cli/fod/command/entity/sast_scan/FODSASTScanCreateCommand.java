package com.fortify.cli.fod.command.entity.sast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "create",
        description = "Create a new SAST scan on FoD."
)
public class FODSASTScanCreateCommand extends DummyCommand {
}
