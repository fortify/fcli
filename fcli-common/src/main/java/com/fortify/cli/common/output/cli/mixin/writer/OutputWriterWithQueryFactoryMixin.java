package com.fortify.cli.common.output.cli.mixin.writer;

import com.fortify.cli.common.output.cli.mixin.impl.OutputOptionsArgGroup;
import com.fortify.cli.common.output.cli.mixin.impl.QueryOptionsArgGroup;
import com.fortify.cli.common.output.query.IQueryExpressionSupplier;
import com.fortify.cli.common.output.query.QueryExpression;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.query.OutputWriterWithQuery;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

public class OutputWriterWithQueryFactoryMixin implements IOutputWriterFactory, IQueryExpressionSupplier {
    @Getter @Setter(onMethod=@__({@Spec(Target.MIXEE)})) private CommandSpec mixee;
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false, order=30)
    private OutputOptionsArgGroup outputOptionsArgGroup = new OutputOptionsArgGroup();
    @ArgGroup(headingKey = "arggroup.query.heading", exclusive = false, order=40)
    private QueryOptionsArgGroup queryOptionsArgGroup = new QueryOptionsArgGroup();
    
    @Override
    public QueryExpression getQueryExpression() {
        return queryOptionsArgGroup.getQueryExpression();
    }
    
    @Override
    public IOutputWriter createOutputWriter(StandardOutputConfig defaultOutputConfig) {
        return new OutputWriterWithQuery(mixee, outputOptionsArgGroup, queryOptionsArgGroup, defaultOutputConfig);
    }
}
