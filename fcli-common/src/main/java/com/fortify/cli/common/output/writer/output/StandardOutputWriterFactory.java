package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;

@ReflectiveAccess
public class StandardOutputWriterFactory implements IOutputWriterFactory {
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false)
    private OutputOptionsArgGroup outputOptionsArgGroup;
    
    @Override
    public IOutputWriter createOutputWriter(CommandSpec mixee, OutputConfig defaultOutputConfig) {
        return new OutputMixin(mixee, outputOptionsArgGroup, defaultOutputConfig);
    }
}
