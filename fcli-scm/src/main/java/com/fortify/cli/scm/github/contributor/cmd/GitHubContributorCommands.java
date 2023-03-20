package com.fortify.cli.scm.github.contributor.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "github-contributor",
        aliases = "gh-contributor",
        subcommands = {
                GitHubContributorListCommand.class
        }
)
public class GitHubContributorCommands extends AbstractFortifyCLICommand {
}
