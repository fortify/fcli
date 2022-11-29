package com.fortify.cli.sc_dast.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.AbstractUnirestOutputCommand;
import com.fortify.cli.sc_dast.rest.cli.mixin.SCDastUnirestRunnerMixin;
import com.fortify.cli.sc_dast.session.manager.ISCDastSessionData;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractSCDastOutputCommand extends AbstractUnirestOutputCommand<ISCDastSessionData> {
    @Getter @Mixin SCDastUnirestRunnerMixin unirestRunner;
}
