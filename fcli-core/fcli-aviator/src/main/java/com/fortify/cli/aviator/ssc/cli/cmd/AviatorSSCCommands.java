package com.fortify.cli.aviator.ssc.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine;

@CommandLine.Command(
        name = "ssc",
        subcommands = {
                AviatorSSCAuditCommand.class,
                AviatorSSCPrepareCommand.class,
                AviatorSSCApplyRemediationsCommand.class
        }

)
public class AviatorSSCCommands extends AbstractContainerCommand {
}
