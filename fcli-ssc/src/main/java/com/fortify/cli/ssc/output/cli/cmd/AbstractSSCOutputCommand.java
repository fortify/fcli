package com.fortify.cli.ssc.output.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.unirest.AbstractUnirestOutputCommand;
import com.fortify.cli.ssc.rest.cli.mixin.SSCUnirestRunnerMixin;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractSSCOutputCommand extends AbstractUnirestOutputCommand {
    @Getter @Mixin SSCUnirestRunnerMixin unirestRunner;
}
