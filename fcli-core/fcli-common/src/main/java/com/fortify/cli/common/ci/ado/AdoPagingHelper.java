/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.ci.ado;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.util.Break;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.PagedList;
import kong.unirest.UnirestInstance;

/**
 * This class provides utility methods for handling Azure DevOps paging.
 * Azure DevOps uses the x-ms-continuationtoken header for pagination.
 * 
 * @author rsenden
 */
public class AdoPagingHelper {
    private AdoPagingHelper() {}
    
    public static final <R extends JsonNode> PagedList<R> pagedRequest(HttpRequest<?> request, Class<R> returnType) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer(), returnType);
    }
    
    /**
     * Create a next page URL producer that looks for the x-ms-continuationtoken header
     * and appends it as a continuationToken query parameter for the next request.
     */
    public static final INextPageUrlProducer nextPageUrlProducer() {
        return (request, response) -> {
            String continuationToken = getContinuationToken(response);
            if (continuationToken == null || continuationToken.isEmpty()) {
                return null;
            }
            // Build next page URL by adding/replacing continuationToken query parameter
            String baseUrl = request.getUrl();
            // Remove existing continuationToken parameter if present
            baseUrl = baseUrl.replaceAll("[?&]continuationToken=[^&]*", "");
            // Add the new continuationToken
            String separator = baseUrl.contains("?") ? "&" : "?";
            return baseUrl + separator + "continuationToken=" + continuationToken;
        };
    }
    
    /**
     * Extract the continuation token from the response headers.
     * Azure DevOps returns this in the x-ms-continuationtoken header.
     */
    private static String getContinuationToken(HttpResponse<?> response) {
        return response.getHeaders().getFirst("x-ms-continuationtoken");
    }
    
    /**
     * Process items from a paged ADO API response with Break support.
     * ADO returns items in a "value" array in the response body.
     * 
     * @param unirest UnirestInstance for making requests
     * @param request Initial request
     * @param itemProcessor Function to process each item, returns Break.TRUE to stop
     */
    public static void processPagedItems(UnirestInstance unirest, HttpRequest<?> request, Function<JsonNode, Break> itemProcessor) {
        PagingHelper.processPagesWithBreak(unirest, request, nextPageUrlProducer(), response -> {
            ObjectNode body = (ObjectNode) response.getBody();
            JsonNode items = body.get("value");
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    if (itemProcessor.apply(item).doBreak()) {
                        return Break.TRUE;
                    }
                }
            }
            return Break.FALSE;
        });
    }
}
