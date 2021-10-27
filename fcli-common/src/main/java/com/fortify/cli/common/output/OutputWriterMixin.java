package com.fortify.cli.common.output;

import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine;

@ReflectiveAccess
public class OutputWriterMixin {
    @CommandLine.Option(names = {"--fmt", "--format"},
            description = "Output format. Possible values: ${COMPLETION-CANDIDATES}.",
            defaultValue = "json", required = false, order=1)
    @Getter
    private OutputFormat format;

    @CommandLine.Option(names = {"--fields"},
            description = "Define the fields to be included in the output, together with optional header names",
            required = false, order=2)
    @Getter
    private String fields;

    public void printToFormat(JsonNode response){
        format.getOutputWriterFactory().createOutputWriter().write(response);
    }

    //TODO: allow printToFormat with (String response)
}
