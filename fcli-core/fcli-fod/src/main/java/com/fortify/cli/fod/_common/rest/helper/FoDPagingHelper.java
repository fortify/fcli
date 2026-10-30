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
package com.fortify.cli.fod._common.rest.helper;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.URIHelper;
import com.fortify.cli.common.util.Break;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;
import kong.unirest.UnirestInstance;

public class FoDPagingHelper {
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer());
    }

    /**
     * Process the items of a paged FoD request one page at a time, without loading all pages
     * into memory first. Processing stops, and no further pages are loaded, as soon as the given
     * processor returns {@link Break#TRUE}.
     */
    public static final void processItems(UnirestInstance unirest, HttpRequest<?> request, Function<JsonNode, Break> processor) {
        PagingHelper.processPagesWithBreak(unirest, request, nextPageUrlProducer(), response -> {
            var items = FoDInputTransformer.getItems(response.getBody());
            if ( items != null && items.isArray() ) {
                for ( var item : items ) {
                    if ( processor.apply(item).doBreak() ) { return Break.TRUE; }
                }
            }
            return Break.FALSE;
        });
    }
    public static final INextPageUrlProducer nextPageUrlProducer() {
        return (req,resp) -> {
            JsonNode body = resp.getBody();
            if ( body.has("offset") && body.has("totalCount") && body.has("limit") ) {
                int offset = body.get("offset").asInt();
                int totalCount = body.get("totalCount").asInt();
                int limit = body.get("limit").asInt();
                int newOffset = offset + limit;
                if (newOffset < totalCount) {
                    return URIHelper.addOrReplaceParam(req.getUrl(), "offset", newOffset);
                }
                return null;
            }
            return null;
        };
    }
}
