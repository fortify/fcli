package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.output.query.IQueryExpressionSupplier;
import com.fortify.cli.common.output.query.QueryExpression;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.query.OutputWriterWithQuery;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;

public class OutputWriterWithQueryFactoryMixin implements IOutputWriterFactory, IQueryExpressionSupplier {
    @Mixin private CommandHelperMixin commandHelper;
    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false, order=30)
    private OutputOptionsArgGroup outputOptionsArgGroup = new OutputOptionsArgGroup();
    @ArgGroup
    private QueryOptionsArgGroup queryOptionsArgGroup = new QueryOptionsArgGroup();
    
    @Override
    public QueryExpression getQueryExpression() {
        return queryOptionsArgGroup.getQueryExpression();
    }
    
    @Override
    public IOutputWriter createOutputWriter(StandardOutputConfig defaultOutputConfig) {
        return new OutputWriterWithQuery(commandHelper.getCommandSpec(), outputOptionsArgGroup, queryOptionsArgGroup, defaultOutputConfig);
    }
}
