package com.fortify.cli.sc_dast.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.AbstractUnirestOutputCommand;
import com.fortify.cli.sc_dast.rest.cli.mixin.SCDastUnirestRunnerMixin;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractSCDastOutputCommand extends AbstractUnirestOutputCommand {
    @Getter @Mixin SCDastUnirestRunnerMixin unirestRunner;
}
