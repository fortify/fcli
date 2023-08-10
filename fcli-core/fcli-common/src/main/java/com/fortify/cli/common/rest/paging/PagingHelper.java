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

import kong.unirest.core.HttpRequest;
import kong.unirest.core.PagedList;

public class PagingHelper {
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request, INextPageUrlProducer nextPageUrlProducer) {
        return pagedRequest(request, nextPageUrlProducer, JsonNode.class);
    }
    
    @SuppressWarnings("unchecked") // TODO Can we get rid of these warnings in a better way?
    public static final <R extends JsonNode> PagedList<R> pagedRequest(HttpRequest<?> request, INextPageUrlProducer nextPageUrlProducer, Class<R> returnType) {
        return request.asPaged(r->r.asObject(returnType), nextPageUrlProducer::getNextPageUrl);
    }
}
