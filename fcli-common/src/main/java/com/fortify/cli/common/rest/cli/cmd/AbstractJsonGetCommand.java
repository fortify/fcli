package com.fortify.cli.common.rest.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractJsonGetCommand extends AbstractJsonOutputCommand {
    public static final String CMD_NAME = "get";
    @Getter @Mixin private OutputMixin outputMixin; 
    
    @Override
    public OutputConfig getBasicOutputConfig() {
        return OutputConfig.details().singular(true);
    }
}
