package com.fortify.cli.ssc.rest.helper;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

public class SSCPagingHelper {
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request) {
        return pagedRequest(request, SSCPagingHelper::getTrue);
    }
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request, Supplier<Boolean> continueSupplier) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer(continueSupplier));
    }
    public static final INextPageUrlProducer nextPageUrlProducer() {
        return nextPageUrlProducer(SSCPagingHelper::getTrue);
    }
    public static final INextPageUrlProducer nextPageUrlProducer(Supplier<Boolean> continueSupplier) {
        return r -> {
            if ( continueSupplier.get() ) {
                JsonNode body = r.getBody();
                try {
                    return JsonHelper.evaluateSpelExpression(body, "links.next.href", String.class);
                } catch ( Exception e ) {} // TODO will JsonHelper.evaluatePath throw an exception if path not found?
            }
            return null;
        };
    }
    private static final boolean getTrue() { return true; }
}
