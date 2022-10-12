package com.fortify.cli.fod.output.cli;

import com.fortify.cli.common.output.cli.cmd.AbstractUnirestOutputCommand;
import com.fortify.cli.fod.rest.cli.mixin.FoDUnirestRunnerMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractFoDOutputCommand extends AbstractUnirestOutputCommand {
    @Getter @Mixin FoDUnirestRunnerMixin unirestRunner;
}
