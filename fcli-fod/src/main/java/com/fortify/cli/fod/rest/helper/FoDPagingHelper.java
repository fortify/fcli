package com.fortify.cli.fod.rest.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;

import io.micronaut.http.uri.UriBuilder;
import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

public class FoDPagingHelper {
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer(request));
    }
    public static final INextPageUrlProducer nextPageUrlProducer(HttpRequest<?> originalRequest) {
        return nextPageUrlProducer(originalRequest.getUrl());
    }
    public static final INextPageUrlProducer nextPageUrlProducer(String uri) {
        return r -> {
            JsonNode body = r.getBody();
            if ( body.has("offset") && body.has("totalCount") && body.has("limit") ) {
                int offset = body.get("offset").asInt();
                int totalCount = body.get("totalCount").asInt();
                int limit = body.get("limit").asInt();
                int newOffset = offset + limit;
                if (newOffset < totalCount) {
                    // UriBuilder supports parsing a String directly but doesn't properly recognize existing request parameters,
                    // so we use an URI instance instead.
                    return UriBuilder.of(uri).replaceQueryParam("offset", newOffset).build().toString();
                }
                return null;
            }
            return null;
        };
    }
}
