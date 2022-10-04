package com.fortify.cli.common.rest.paging;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.PagedList;

public class PagingHelper {
    @SuppressWarnings("unchecked") // TODO Can we get rid of this warning in a better way?
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request, Function<HttpResponse<JsonNode>, String> nextPageUrlProducer) {
        return request.asPaged(r->r.asObject(JsonNode.class), nextPageUrlProducer);
    }
}
