package com.fortify.cli.ssc.rest.bulk;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.UnirestInstance;

/**
 * Interface for adding one or more requests and corresponding response consumers
 * to a given {@link SSCBulkRequestBuilder} to embed additional entities into a 
 * given JsonNode. Implementations would usually invoke the 
 * {@link SSCBulkRequestBuilder#request(kong.unirest.HttpRequest, java.util.function.Consumer)}
 * method (or similar method taking a {@link Consumer}) with a {@link Consumer}
 * that takes the response of the request as input and updates the given record
 * to embed response data.
 * 
 * @author rsenden
 */
@FunctionalInterface
public interface ISSCEntityEmbedder {
    void addEmbedRequests(SSCBulkRequestBuilder builder, UnirestInstance unirest, JsonNode record);
}