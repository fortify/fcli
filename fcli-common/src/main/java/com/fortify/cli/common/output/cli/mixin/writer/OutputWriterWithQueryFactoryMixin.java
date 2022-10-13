package com.fortify.cli.common.output.cli.mixin.writer;

import java.util.List;

import com.fortify.cli.common.output.cli.mixin.impl.OutputOptionsArgGroup;
import com.fortify.cli.common.output.cli.mixin.impl.QueryOptionsArgGroup;
import com.fortify.cli.common.output.query.IOutputQueriesSupplier;
import com.fortify.cli.common.output.query.OutputQuery;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.query.OutputWriterWithQuery;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

@ReflectiveAccess
public class OutputWriterWithQueryFactoryMixin implements IOutputWriterFactory, IOutputQueriesSupplier {
    @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false)
    private OutputOptionsArgGroup outputOptionsArgGroup = new OutputOptionsArgGroup();
    @ArgGroup(headingKey = "arggroup.query.heading", exclusive = false)
    private QueryOptionsArgGroup queryOptionsArgGroup;
    
    @Override
    public List<OutputQuery> getOutputQueries() {
        return queryOptionsArgGroup==null ? null : queryOptionsArgGroup.getOutputQueries();
    }
    
    @Override
    public IOutputWriter createOutputWriter(StandardOutputConfig defaultOutputConfig) {
        return new OutputWriterWithQuery(mixee, outputOptionsArgGroup, queryOptionsArgGroup, defaultOutputConfig);
    }
}
