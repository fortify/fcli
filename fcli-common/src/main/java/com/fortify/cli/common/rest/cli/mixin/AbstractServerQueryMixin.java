package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.output.cli.mixin.AbstractOutputHelperMixin;
import com.fortify.cli.common.output.query.IQueryExpressionSupplier;
import com.fortify.cli.common.output.query.QueryExpression;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;

@Command
public class AbstractServerQueryMixin implements IQueryExpressionSupplier {
    @Getter @Mixin private CommandHelperMixin commandHelper;
    
    @Override
    public QueryExpression getQueryExpression() {
        CommandSpec spec = commandHelper.getCommandSpec();
        CommandSpec outputHelperMixin = spec.mixins().get("outputHelper");
        if ( outputHelperMixin==null ) {
            throw new RuntimeException("Command must provide outputHelper mixin: "+spec.userObject().getClass().getName());
        }
        var mixinInstance = (AbstractOutputHelperMixin)outputHelperMixin.userObject();
        var outputWriterFactory = mixinInstance.getOutputWriterFactory();
        return outputWriterFactory instanceof IQueryExpressionSupplier 
                ? ((IQueryExpressionSupplier)outputWriterFactory).getQueryExpression()
                : null;
    }
}
