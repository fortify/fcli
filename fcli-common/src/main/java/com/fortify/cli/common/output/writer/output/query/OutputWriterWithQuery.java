package com.fortify.cli.common.output.writer.output.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.writer.OutputWriterWithQueryFactoryMixin;
import com.fortify.cli.common.output.query.IQueryExpressionSupplier;
import com.fortify.cli.common.output.query.QueryExpression;
import com.fortify.cli.common.output.writer.output.standard.IOutputOptions;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputWriter;

import picocli.CommandLine.Model.CommandSpec;

/**
 * TODO Refactor this class once all commands have been refactored to use {@link UnirestOutputHelperMixins};
 *      all picocli annotatations should be removed, as they will be passed by {@link OutputWriterWithQueryFactoryMixin}
 *      through our constructor. As this class by then will no longer be a mixin, it should be renamed
 *      to OutputWriterWithQuery, and moved to the writer.output.query package.
 * @author rsenden
 *
 */
public class OutputWriterWithQuery extends StandardOutputWriter {
    private final IQueryExpressionSupplier queryExpressionSupplier;
    
    public OutputWriterWithQuery(CommandSpec mixee, IOutputOptions outputOptions, IQueryExpressionSupplier queryExpressionSupplier, StandardOutputConfig defaultOutputConfig) {
        super(mixee, outputOptions, defaultOutputConfig);
        this.queryExpressionSupplier = queryExpressionSupplier;
    }
    
    @Override
    protected JsonNode applyRecordOutputFilters(OutputFormat outputFormat, JsonNode record) {
        QueryExpression queryExpression = queryExpressionSupplier.getQueryExpression();
        return queryExpression==null || queryExpression.matches(record)
                ? record : null;
    }
}
