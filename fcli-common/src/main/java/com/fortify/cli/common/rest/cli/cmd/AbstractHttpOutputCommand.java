package com.fortify.cli.common.rest.cli.cmd;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;

/**
 * This abstract base class allows for outputting data produced by an arbitrary
 * {@link HttpRequest} using a provided {@link OutputMixin} instance. Subclasses 
 * implement or override the various protected methods to define actual behavior.
 *   
 * @author rsenden
 */
@ReflectiveAccess
public abstract class AbstractHttpOutputCommand extends AbstractUnirestRunnerOutputCommand {
    /**
     * This method generates a request by calling the {@link #getBaseRequest(UnirestInstance)}
     * method, updates that request by calling the {@link #updateRequest(HttpRequest)} method, 
     * then calls {@link OutputMixin#write(HttpRequest, Function)} on the provided {@link OutputMixin} 
     * to execute the request and output the response, using the optional next page producer as 
     * returned by the {@link #getNextPageUrlProducer(UnirestInstance, HttpRequest)} method.
     */
    @Override
    protected final void generateAndWriteOutput(OutputMixin outputMixin, UnirestInstance unirest) {
        HttpRequest<?> request = updateRequest(getBaseRequest(unirest));
        outputMixin.write(request, getNextPageUrlProducer(unirest, request));
    }
    
    /**
     * Subclasses need to implement this method to return the base request to
     * be executed. This request may be updated by the {@link #updateRequest(HttpRequest)}
     * method before being executed, and may be used as the basis for generating
     * paged requests through the {@link #getNextPageUrlProducer(UnirestInstance, HttpRequest)}
     * method.
     * @param unirest {@link UnirestInstance} used to create the request.
     * @return {@link HttpRequest} to be executed
     */
    protected abstract HttpRequest<?> getBaseRequest(UnirestInstance unirest);
    
    /**
     * This method may be overridden to update the request generated by the 
     * {@link #getBaseRequest(UnirestInstance)} method. This method is usually
     * overridden by (abstract) base classes to provide generic functionality
     * for all their subclasses, for example for adding generic request parameters.
     * This default implementation returns the provided request as-is.
     * @param request {@link HttpRequest} generated by {@link #getBaseRequest(UnirestInstance)}
     * @return Optionally updated {@link HttpRequest} instance
     */
    protected HttpRequest<?> updateRequest(HttpRequest<?> request) {
        return request;
    }
    
    /**
     * This method may be overridden to provide a next page producer function.
     * This default implementation returns {@code null}, which disables paging.
     * @param unirest {@link UnirestInstance} that may optionally be used to generate the next page producer function
     * @param originalRequest Original {@link HttpRequest} that may optionally be used to generate the next page producer function
     * @return Next page producer function, or null to disable paging
     */
    protected Function<HttpResponse<JsonNode>, String> getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
        return null;
    }
}