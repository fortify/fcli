package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputWriter;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;

public class StandardOutputWriterFactoryMixin implements IOutputWriterFactory {
    @Mixin private CommandHelperMixin commandHelper;
    
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false, order=30)
    private OutputOptionsArgGroup outputOptionsArgGroup  = new OutputOptionsArgGroup();
    
    @Override
    public IOutputWriter createOutputWriter(StandardOutputConfig defaultOutputConfig) {
        return new StandardOutputWriter(commandHelper.getCommandSpec(), outputOptionsArgGroup, defaultOutputConfig);
    }
}
