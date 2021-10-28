package com.fortify.cli.common.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.filter.JsonPathOutputFilter;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Given that this class contains picocli-related code, it should be moved to the appropriate package under the command/picocli package 
 * 
 */
@ReflectiveAccess
public class OutputFilterOptions {
    @Option(names = {"--JSONPath"}, description = "Filters output using JSONPath", order = 1)
    @Getter private String jsonPath;

//    @Option(names = {"--XPath"}, description = "Filter using XPath", order = 2)
//    @Getter private String xPath;

    public JsonNode filterOutput(JsonNode response){
        return JsonPathOutputFilter.filter(response, jsonPath);
    }

}
