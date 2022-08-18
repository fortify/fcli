package com.fortify.cli.ssc.picocli.command.appversion;

import picocli.CommandLine.Command;

@Command(
        name = "appversion",
        subcommands = {
        	SSCAppVersionListCommand.class,
            SSCAppVersionGetCommand.class,
            SSCAppVersionCreateWizardCommand.class
        }
)
public class SSCAppVersionCommands {
}
