package com.fortify.cli.fod.command.entity.application;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "update",
        description = "Update an existing application on FoD."
)
public class FODApplicationUpdateCommand  extends DummyCommand {
}
