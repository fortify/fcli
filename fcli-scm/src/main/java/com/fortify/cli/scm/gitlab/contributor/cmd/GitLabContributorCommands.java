package com.fortify.cli.scm.gitlab.contributor.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;

import picocli.CommandLine.Command;

@Command(
        name = "gitlab-contributor",
        aliases = "gl-contributor",
        subcommands = {
                GitLabContributorListCommand.class
        }
)
public class GitLabContributorCommands extends AbstractFortifyCLICommand {
}
