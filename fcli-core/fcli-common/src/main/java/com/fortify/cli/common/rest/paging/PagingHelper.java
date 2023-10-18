/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.rest.paging;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.Header;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.PagedList;
import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

public class PagingHelper {
    /**
     * Return a Unirest {@link PagedList} based on the given base request and {@link INextPageUrlProducer}.
     * Note that Unirest first collects all responses in memory before returning the {@link PagedList}. To 
     * (potentially) reduce memory usage and allow for results to be processed immediately after each page 
     * has been loaded (for example for streaming output), it may be better to handle paging manually using
     * {@link INextPageRequestProducer}. An {@link INextPageUrlProducer} instance can be converted to an
     * {@link INextPageRequestProducer} instance using the {@link #asNextPageRequestProducer(UnirestInstance, INextPageUrlProducer)}
     * method. 
     * @param request
     * @param nextPageUrlProducer
     * @return
     */
    @SuppressWarnings("unchecked") // TODO Can we get rid of these warnings in a better way?
    public static final <R extends JsonNode> PagedList<R> pagedRequest(HttpRequest<?> request, INextPageUrlProducer nextPageUrlProducer, Class<R> returnType) {
        return request.asPaged(r->r.asObject(returnType), response->nextPageUrlProducer.getNextPageUrl(request, response));
    }
    
    /**
     * Same as {@link #pagedRequest(HttpRequest, INextPageUrlProducer, Class)} (same considerations apply),
     * but with fixed {@link JsonNode}-based return type. 
     * @param request
     * @param nextPageUrlProducer
     * @return
     */
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request, INextPageUrlProducer nextPageUrlProducer) {
        return pagedRequest(request, nextPageUrlProducer, JsonNode.class);
    }
    
    /**
     * Return an {@link INextPageRequestProducer} instance based on the given {@link INextPageUrlProducer},
     * using the given {@link UnirestInstance} to produce requests for loading next pages.    
     * @param unirest
     * @param nextPageUrlProducer
     * @return
     */
    public static final INextPageRequestProducer asNextPageRequestProducer(UnirestInstance unirest, INextPageUrlProducer nextPageUrlProducer) {
        return unirest==null || nextPageUrlProducer==null ? null : new NextPageRequestProducer(unirest, nextPageUrlProducer);
    }
    
    @RequiredArgsConstructor
    private static final class NextPageRequestProducer implements INextPageRequestProducer {
        private final UnirestInstance unirest;
        private final INextPageUrlProducer nextPageUrlProducer;
        
        @Override
        public HttpRequest<?> getNextPageRequest(HttpRequest<?> request, HttpResponse<? extends JsonNode> jsonResponse) {
            var nextPageUrl = nextPageUrlProducer.getNextPageUrl(request, jsonResponse);
            // TODO Any more request attributes to be copied from original request?
            return nextPageUrl==null ? null : nextPageRequest(request, nextPageUrl); 
        }

        private HttpRequest<?> nextPageRequest(HttpRequest<?> originalRequest, String nextPageUrl) {
            HttpRequest<?> result = unirest.request(originalRequest.getHttpMethod().name(), nextPageUrl)
                    .socketTimeout(originalRequest.getSocketTimeout())
                    .connectTimeout(originalRequest.getConnectTimeout())
                    .proxy(originalRequest.getProxy());
            for (Header header : originalRequest.getHeaders().all() ) {
                result.headerReplace(header.getName(), header.getValue());
            }
            return result;
        }
        
        
    }
}
