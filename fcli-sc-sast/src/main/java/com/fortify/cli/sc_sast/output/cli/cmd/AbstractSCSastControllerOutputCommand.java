package com.fortify.cli.sc_sast.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.AbstractUnirestOutputCommand;
import com.fortify.cli.sc_sast.rest.cli.mixin.SCSastControllerUnirestRunnerMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractSCSastControllerOutputCommand extends AbstractUnirestOutputCommand {
    @Getter @Mixin SCSastControllerUnirestRunnerMixin unirestRunner;
}
