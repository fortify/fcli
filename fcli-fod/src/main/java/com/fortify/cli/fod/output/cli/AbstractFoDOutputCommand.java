package com.fortify.cli.fod.output.cli;

import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.fod.rest.cli.mixin.FoDUnirestRunnerMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractFoDOutputCommand extends AbstractOutputCommand {
    @Getter @Mixin FoDUnirestRunnerMixin unirestRunner;
}
