package com.fortify.cli.common.rest.paging;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

@SuppressWarnings("unchecked") // TODO Can we get rid of these warnings in a better way?
public class PagingHelper {
    
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request, INextPageUrlProducer nextPageUrlProducer) {
        return pagedRequest(request, nextPageUrlProducer, JsonNode.class);
    }
    
    public static final <R extends JsonNode> PagedList<R> pagedRequest(HttpRequest<?> request, INextPageUrlProducer nextPageUrlProducer, Class<R> returnType) {
        return request.asPaged(r->r.asObject(returnType), nextPageUrlProducer::getNextPageUrl);
    }
}
