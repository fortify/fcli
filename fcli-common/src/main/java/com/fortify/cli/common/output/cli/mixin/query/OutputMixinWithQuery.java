package com.fortify.cli.common.output.cli.mixin.query;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.output.writer.OutputFormat;
import com.fortify.cli.common.output.writer.output.OutputOptionsArgGroup;
import com.fortify.cli.common.output.writer.output.query.OutputQuery;
import com.fortify.cli.common.output.writer.output.query.OutputWriterWithQueryFactory;
import com.fortify.cli.common.output.writer.output.query.QueryOptionsArgGroup;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;

/**
 * TODO Refactor this class once all commands have been refactored to use {@link OutputHelperMixins};
 *      all picocli annotatations should be removed, as they will be passed by {@link OutputWriterWithQueryFactory}
 *      through our constructor. As this class by then will no longer be a mixin, it should be renamed
 *      to OutputWriterWithQuery, and moved to the writer.output.query package.
 * @author rsenden
 *
 */
@ReflectiveAccess
public class OutputMixinWithQuery extends OutputMixin {
    // TODO Remove ArgGroup annotation, make final, use IOutputQueriesSupplier instead once command refactoring is complete
    @ArgGroup(headingKey = "arggroup.query.heading", exclusive = false)
    private QueryOptionsArgGroup queryOptionsArgGroup;
    
    // TODO Remove once command refactoring is complete
    public OutputMixinWithQuery() {
        super();
    }

    public OutputMixinWithQuery(CommandSpec mixee, OutputOptionsArgGroup outputOptionsArgGroup, QueryOptionsArgGroup queryOptionsArgGroup, OutputConfig defaultOutputConfig) {
        super(mixee, outputOptionsArgGroup, defaultOutputConfig);
        this.queryOptionsArgGroup = queryOptionsArgGroup;
    }

    // TODO Make private once command refactoring is complete
    public List<OutputQuery> getOutputQueries() {
        return queryOptionsArgGroup==null ? Collections.emptyList() : queryOptionsArgGroup.getOutputQueries();
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
