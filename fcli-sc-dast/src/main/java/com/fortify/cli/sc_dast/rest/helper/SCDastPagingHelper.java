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
package com.fortify.cli.sc_dast.rest.helper;

import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;

import io.micronaut.http.uri.UriBuilder;
import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

public class SCDastPagingHelper {
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer(request));
    }
    public static final INextPageUrlProducer nextPageUrlProducer(HttpRequest<?> originalRequest) {
        return nextPageUrlProducer(originalRequest.getUrl());
    }
    public static final INextPageUrlProducer nextPageUrlProducer(String uri) {
        return r -> {
            JsonNode body = r.getBody();
            if ( body.has("offset") && body.has("totalItems") && body.has("limit") ) {
                int offset = body.get("offset").asInt();
                int totalCount = body.get("totalItems").asInt();
                int limit = body.get("limit").asInt();
                int newOffset = offset + limit;
                if (newOffset < totalCount) {
                    // UriBuilder supports parsing a String directly but doesn't properly recognize existing request parameters,
                    // so we use an URI instance instead.
                    return UriBuilder.of(URI.create(uri)).replaceQueryParam("offset", newOffset).build().toString();
                }
            }
            return null;
        };
    }
}
