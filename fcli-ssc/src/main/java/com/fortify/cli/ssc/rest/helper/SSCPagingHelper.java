package com.fortify.cli.ssc.rest.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

public class SSCPagingHelper {
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer());
    }
    public static final INextPageUrlProducer nextPageUrlProducer() {
        return r -> {
            JsonNode body = r.getBody();
            try {
                return JsonHelper.evaluateSpELExpression(body, "links.next.href", String.class);
            } catch ( Exception e ) {} // TODO will JsonHelper.evaluatePath throw an exception if path not found?
            return null;
        };
    }
}
