package com.fortify.cli.aviator.project.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "project",
        subcommands = {
                AviatorProjectCreateCommand.class,
                AviatorProjectDeleteCommand.class,
                AviatorProjectGetCommand.class,
                AviatorProjectListCommand.class,
                AviatorProjectUpdateCommand.class
        }
)
public class AviatorProjectCommands extends AbstractContainerCommand {
}