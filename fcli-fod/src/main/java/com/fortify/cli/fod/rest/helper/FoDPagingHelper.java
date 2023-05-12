package com.fortify.cli.fod.rest.helper;

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.LinkHeaderPagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

public class FoDPagingHelper {
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request) {
        return LinkHeaderPagingHelper.pagedRequest(request, nextPageUrlProducer(request));
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
                    // UriBuilder was only appending parameters with ? not &
                    // return UriBuilder.of(uri).replaceQueryParam("offset", newOffset).build().toString();
                    try {
                        return appendUri(uri, "offset=" + newOffset).toString();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
            return null;
        };
    }
    
    private static final URI appendUri(String uri, String appendQuery) throws URISyntaxException {
        URI oldUri = new URI(uri);

        String newQuery = oldUri.getQuery();
        if (newQuery == null) {
            newQuery = appendQuery;
        } else {
            newQuery += "&" + appendQuery;
        }

        return new URI(oldUri.getScheme(), oldUri.getAuthority(),
                oldUri.getPath(), newQuery, oldUri.getFragment());
    }
}
