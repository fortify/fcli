package com.fortify.cli.common.rest.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.rest.cli.mixin.DeleteAliasMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractHttpDeleteCommand extends AbstractHttpOutputCommand {
    public static final String CMD_NAME = "delete";
    @Mixin private DeleteAliasMixin aliasMixin; // For some reason this doesn't seem to do anything; see https://github.com/remkop/picocli/issues/1836
    @Getter @Mixin private OutputMixin outputMixin; 
    
    @Override
    public OutputConfig getBasicOutputConfig() {
        return OutputConfig.table();
    }
}
