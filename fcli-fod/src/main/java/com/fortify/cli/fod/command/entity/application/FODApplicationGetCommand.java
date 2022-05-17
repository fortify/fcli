package com.fortify.cli.fod.command.entity.application;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "get",
        description = "Get an application from FoD."
)
public class FODApplicationGetCommand  extends DummyCommand {
}
