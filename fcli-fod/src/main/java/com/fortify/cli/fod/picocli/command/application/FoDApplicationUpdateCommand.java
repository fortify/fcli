package com.fortify.cli.fod.picocli.command.application;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "update",
        description = "Update an existing application on FoD."
)
public class FoDApplicationUpdateCommand  extends DummyCommand {
}
