package com.fortify.cli.scm.github.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.AbstractUnirestOutputCommand;
import com.fortify.cli.scm.github.cli.mixin.GitHubUnirestRunnerMixin;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractGitHubOutputCommand extends AbstractUnirestOutputCommand {
    @Getter @Mixin GitHubUnirestRunnerMixin unirestRunner;
}
