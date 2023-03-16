package com.fortify.cli.common.output.cli.mixin.writer;

import com.fortify.cli.common.output.cli.mixin.impl.OutputOptionsArgGroup;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputWriter;

import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

public class StandardOutputWriterFactoryMixin implements IOutputWriterFactory {
    @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false, order=30)
    private OutputOptionsArgGroup outputOptionsArgGroup  = new OutputOptionsArgGroup();
    
    @Override
    public IOutputWriter createOutputWriter(StandardOutputConfig defaultOutputConfig) {
        return new StandardOutputWriter(mixee, outputOptionsArgGroup, defaultOutputConfig);
    }
}
