package com.fortify.cli.common.rest.cli.cmd;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

/**
 * This abstract base class allows for outputting data produced by the 
 * {@link #generateOutput(UnirestInstance)} method using a provided 
 * {@link OutputMixin} instance. Subclasses implement or override the 
 * various protected methods to define actual behavior.
 *   
 * @author rsenden
 */
@ReflectiveAccess
public abstract class AbstractJsonOutputCommand extends AbstractUnirestRunnerOutputCommand {
    /**
     * This method generates a request by calling the {@link #getBaseRequest(UnirestInstance)}
     * method, updates that request by calling the {@link #updateRequest(HttpRequest)} method, 
     * then calls {@link OutputMixin#write(HttpRequest, Function)} on the provided {@link OutputMixin} 
     * to execute the request and output the response, using the optional next page producer as 
     * returned by the {@link #getNextPageUrlProducer(UnirestInstance, HttpRequest)} method.
     */
    @Override
    protected final void generateAndWriteOutput(OutputMixin outputMixin, UnirestInstance unirest) {
        outputMixin.write(generateJsonNode(unirest));
    }
    
    /**
     * Subclasses need to implement this method to return a {@link JsonNode} instance
     * to be written using {@link OutputMixin}.
     * 
     * @param unirest {@link UnirestInstance} which may be used to generate the {@link JsonNode}
     * @return Generated {@link JsonNode} instance
     */
    protected abstract JsonNode generateJsonNode(UnirestInstance unirest);
}