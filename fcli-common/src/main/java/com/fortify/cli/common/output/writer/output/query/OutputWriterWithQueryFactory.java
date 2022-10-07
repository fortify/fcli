package com.fortify.cli.common.output.writer.output.query;

import java.util.List;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.query.OutputMixinWithQuery;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.OutputOptionsArgGroup;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

@ReflectiveAccess
public class OutputWriterWithQueryFactory implements IOutputWriterFactory, IOutputQueriesSupplier {
    @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false)
    private OutputOptionsArgGroup outputOptionsArgGroup;
    @ArgGroup(headingKey = "arggroup.query.heading", exclusive = false)
    private QueryOptionsArgGroup queryOptionsArgGroup;
    
    @Override
    public List<OutputQuery> getOutputQueries() {
        return queryOptionsArgGroup==null ? null : queryOptionsArgGroup.getOutputQueries();
    }
    
    @Override
    public IOutputWriter createOutputWriter(OutputConfig defaultOutputConfig) {
        return new OutputMixinWithQuery(mixee, outputOptionsArgGroup, queryOptionsArgGroup, defaultOutputConfig);
    }
}
