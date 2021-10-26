package com.fortify.cli.common.output;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine;

@ReflectiveAccess
public class OutputFilterOptions {
    @CommandLine.Option(names = {"--filter-language"}, description = "Filters output using any of this language: ${COMPLETION-CANDIDATES}", order = 2)
    @Getter private OutputFilter filter;

    @CommandLine.Option(names = {"--expression"}, description = "Filter output expression", order = 1)
    @Getter private String expression;

    public JsonNode filterOutput(JsonNode response){
        return filter.getOutputFilterFactory().createOutputFilter().filter(response, expression);
    }

}
