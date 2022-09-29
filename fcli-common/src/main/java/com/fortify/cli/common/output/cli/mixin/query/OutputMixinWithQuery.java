package com.fortify.cli.common.output.cli.mixin.query;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.output.writer.OutputFormat;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;

@ReflectiveAccess
public class OutputMixinWithQuery extends OutputMixin {
    @ArgGroup(headingKey = "arggroup.query.heading", exclusive = false)
    private QueryOptionsArgGroup queryOptionsArgGroup;
    
    private static final class QueryOptionsArgGroup {
        @CommandLine.Option(names = {"-q", "--query"}, order=1, converter = OutputQueryConverter.class, paramLabel = "<prop><op><value>")
        private List<OutputQuery> outputQueries = Collections.emptyList();
    }
    
    public List<OutputQuery> getOutputQueries() {
        return queryOptionsArgGroup==null ? Collections.emptyList() : queryOptionsArgGroup.outputQueries;
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
