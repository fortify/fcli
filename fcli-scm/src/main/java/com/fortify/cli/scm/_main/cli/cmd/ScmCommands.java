package com.fortify.cli.scm._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.scm.github.contributor.cmd.GitHubContributorCommands;
import com.fortify.cli.scm.gitlab.contributor.cmd.GitLabContributorCommands;

import picocli.CommandLine.Command;

@Command(
        name = "scm",
        resourceBundle = "com.fortify.cli.scm.i18n.ScmMessages",
        subcommands = {
                GitHubContributorCommands.class,
                GitLabContributorCommands.class
        }
)
public class ScmCommands extends AbstractFortifyCLICommand {
}
