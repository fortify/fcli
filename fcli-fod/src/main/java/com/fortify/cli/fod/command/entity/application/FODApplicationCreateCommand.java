package com.fortify.cli.fod.command.entity.application;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "create",
        description = "Create a new application (with a release) from FoD."
)
public class FODApplicationCreateCommand  extends DummyCommand {
}
