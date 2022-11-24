package com.fortify.cli.ssc.activity_feed.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "activity-feed",
        subcommands = {
                SSCActivityFeedListCommand.class
        }
)
public class SSCActivityFeedCommands extends AbstractFortifyCLICommand {
}
