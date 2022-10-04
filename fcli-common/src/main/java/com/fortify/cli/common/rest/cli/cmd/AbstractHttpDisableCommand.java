package com.fortify.cli.common.rest.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractHttpDisableCommand extends AbstractHttpOutputCommand {
    public static final String CMD_NAME = "disable";
    @Getter @Mixin private OutputMixin outputMixin; 
    
    @Override
    public OutputConfig getBasicOutputConfig() {
        return OutputConfig.table();
    }
}
