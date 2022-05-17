package com.fortify.cli.fod.command.entity.dast_scan;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "create",
        description = "Create a new DAST scan on FoD."
)
public class FODDASTScanCreateCommand  extends DummyCommand {
}
