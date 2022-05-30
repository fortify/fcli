package com.fortify.cli.fod.picocli.command.sast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "create",
        description = "Create a new SAST scan on FoD."
)
public class FoDSastScanCreateCommand extends DummyCommand {
}
