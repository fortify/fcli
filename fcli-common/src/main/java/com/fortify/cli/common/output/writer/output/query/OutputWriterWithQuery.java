package com.fortify.cli.common.output.writer.output.query;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.cli.mixin.UnirestOutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.writer.OutputWriterWithQueryFactoryMixin;
import com.fortify.cli.common.output.query.IOutputQueriesSupplier;
import com.fortify.cli.common.output.query.OutputQuery;
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
    private final IOutputQueriesSupplier outputQueriesSupplier;
    
    public OutputWriterWithQuery(CommandSpec mixee, IOutputOptions outputOptions, IOutputQueriesSupplier outputQueriesSupplier, StandardOutputConfig defaultOutputConfig) {
        super(mixee, outputOptions, defaultOutputConfig);
        this.outputQueriesSupplier = outputQueriesSupplier;
    }

    private List<OutputQuery> getOutputQueries() {
        return outputQueriesSupplier==null ? Collections.emptyList() : outputQueriesSupplier.getOutputQueries();
    }
    
    @Override
    protected JsonNode applyRecordOutputFilters(OutputFormat outputFormat, JsonNode record) {
        List<OutputQuery> outputQueries = getOutputQueries();
        return outputQueries==null 
                || outputQueries.isEmpty() 
                || outputQueries.stream().allMatch(q->q.matches(record))
                ? record
                : null;
    }
}
