package com.fortify.cli.common.output.writer.output.query;

import java.util.List;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.query.OutputMixinWithQuery;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.OutputOptionsArgGroup;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;

@ReflectiveAccess
public class OutputWriterWithQueryFactory implements IOutputWriterFactory, IOutputQueriesSupplier {
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false)
    private OutputOptionsArgGroup outputOptionsArgGroup;
    @ArgGroup(headingKey = "arggroup.query.heading", exclusive = false)
    private QueryOptionsArgGroup queryOptionsArgGroup;
    
    @Override
    public List<OutputQuery> getOutputQueries() {
        return queryOptionsArgGroup==null ? null : queryOptionsArgGroup.getOutputQueries();
    }
    
    @Override
    public IOutputWriter createOutputWriter(CommandSpec mixee, OutputConfig defaultOutputConfig) {
        return new OutputMixinWithQuery(mixee, outputOptionsArgGroup, queryOptionsArgGroup, defaultOutputConfig);
    }
}
