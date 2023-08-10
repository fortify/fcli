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
package com.fortify.cli.ssc._common.rest.helper;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;

import kong.unirest.core.HttpRequest;
import kong.unirest.core.PagedList;
import lombok.Setter;

public class SSCPagingHelper {
    private static final SSCContinueNextPageSupplier continueNextPageSupplier = new SSCContinueNextPageSupplier();
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request) {
        return pagedRequest(request, continueNextPageSupplier);
    }
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request, Supplier<Boolean> continueSupplier) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer(continueSupplier));
    }
    public static final INextPageUrlProducer nextPageUrlProducer() {
        return nextPageUrlProducer(continueNextPageSupplier);
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
    public static final class SSCContinueNextPageSupplier implements Supplier<Boolean> {
        @Setter private boolean loadNextPage = true;
        @Override
        public Boolean get() { return loadNextPage; }
    }
}
