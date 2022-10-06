package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

@ReflectiveAccess
public class StandardOutputWriterFactory implements IOutputWriterFactory {
    @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false)
    private OutputOptionsArgGroup outputOptionsArgGroup;
    
    @Override
    public IOutputWriter createOutputWriter(OutputConfig defaultOutputConfig) {
        return new OutputMixin(mixee, outputOptionsArgGroup, defaultOutputConfig);
    }
}
