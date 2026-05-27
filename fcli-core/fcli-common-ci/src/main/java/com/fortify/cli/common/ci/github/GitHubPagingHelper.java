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
package com.fortify.cli.common.ci.github;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.LinkHeaderNextPageUrlProducerFactory;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.util.Break;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;
import kong.unirest.UnirestInstance;

/**
 * This class provides utility methods for handling GitHub paging.
 * 
 * @author rsenden
 */
public class GitHubPagingHelper {
    private GitHubPagingHelper() {}
    
    public static final <R extends JsonNode> PagedList<R> pagedRequest(HttpRequest<?> request, Class<R> returnType) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer(), returnType);
    }
    
    public static final INextPageUrlProducer nextPageUrlProducer() {
        return LinkHeaderNextPageUrlProducerFactory.nextPageUrlProducer("Link", "next");
    }
    
    /**
     * Process items from a paged GitHub API response with Break support.
     * GitHub returns items directly as an array in the response body.
     * 
     * @param unirest UnirestInstance for making requests
     * @param request Initial request
     * @param itemProcessor Function to process each item, returns Break.TRUE to stop
     */
    public static void processPagedItems(UnirestInstance unirest, HttpRequest<?> request, Function<JsonNode, Break> itemProcessor) {
        PagingHelper.processPagesWithBreak(unirest, request, nextPageUrlProducer(), response -> {
            JsonNode body = response.getBody();
            if (body.isArray()) {
                for (JsonNode item : body) {
                    if (itemProcessor.apply(item).doBreak()) {
                        return Break.TRUE;
                    }
                }
            }
            return Break.FALSE;
        });
    }
}
