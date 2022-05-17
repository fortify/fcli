package com.fortify.cli.fod.command.entity.application;

import com.fortify.cli.common.picocli.command.DummyCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "list",
        description = "List all applications on FoD."
)
public class FODApplicationListCommand  extends DummyCommand {
}
