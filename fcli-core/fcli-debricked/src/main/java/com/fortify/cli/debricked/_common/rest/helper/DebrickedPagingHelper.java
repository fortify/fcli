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
package com.fortify.cli.debricked._common.rest.helper;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

public final class DebrickedPagingHelper {
    
    private DebrickedPagingHelper() {}
    
    public static final <T extends HttpRequest<?>> PagedList<JsonNode> pagedRequest(T request, Function<JsonNode, String> nextPageUrlProducer) {
        // For now, implement a no-op paging helper that just returns the response as a single page
        // This can be enhanced later when Debricked API paging requirements are better understood
        return PagingHelper.pagedRequest(request, new DebrickedNextPageUrlProducer(nextPageUrlProducer));
    }
    
    private static final class DebrickedNextPageUrlProducer implements INextPageUrlProducer {
        private final Function<JsonNode, String> nextPageUrlProducer;
        
        public DebrickedNextPageUrlProducer(Function<JsonNode, String> nextPageUrlProducer) {
            this.nextPageUrlProducer = nextPageUrlProducer;
        }
        
        @Override
        public String getNextPageUrl(kong.unirest.HttpRequest<?> request, kong.unirest.HttpResponse<? extends JsonNode> response) {
            return nextPageUrlProducer != null ? nextPageUrlProducer.apply(response.getBody()) : null;
        }
    }
    
    public static final ArrayNode getItems(JsonNode pageNode) {
        // Simple implementation that assumes the response is already an array or contains items directly
        if (pageNode.isArray()) {
            return (ArrayNode) pageNode;
        } else if (pageNode.has("items")) {
            return JsonHelper.evaluateSpelExpression(pageNode, "items", ArrayNode.class);
        } else {
            // Return empty array if no items found
            return JsonHelper.getObjectMapper().createArrayNode();
        }
    }
}