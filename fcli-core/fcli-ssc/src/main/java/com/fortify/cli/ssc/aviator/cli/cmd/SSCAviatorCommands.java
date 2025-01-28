package com.fortify.cli.ssc.aviator.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "aviator",
        subcommands = {
                SSCAviatorAuditCommand.class
        }

)
public class SSCAviatorCommands extends AbstractContainerCommand {
}
