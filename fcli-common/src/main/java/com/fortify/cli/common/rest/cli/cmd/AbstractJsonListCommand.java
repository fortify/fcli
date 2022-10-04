package com.fortify.cli.common.rest.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.query.OutputMixinWithQuery;
import com.fortify.cli.common.rest.cli.mixin.ListAliasMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractJsonListCommand extends AbstractJsonOutputCommand {
    public static final String CMD_NAME = "list";
    @Mixin private ListAliasMixin aliasMixin; // For some reason this doesn't seem to do anything; see https://github.com/remkop/picocli/issues/1836
    @Getter @Mixin private OutputMixinWithQuery outputMixin; 
    
    @Override
    public OutputConfig getBasicOutputConfig() {
        return OutputConfig.table();
    }
}
