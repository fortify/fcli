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
package com.fortify.cli.sc_dast._common.rest.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.URIHelper;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

public class SCDastPagingHelper {
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer());
    }
    public static final INextPageUrlProducer nextPageUrlProducer() {
        return (req,resp) -> {
            JsonNode body = resp.getBody();
            if ( body.has("offset") && body.has("totalItems") && body.has("limit") ) {
                int offset = body.get("offset").asInt();
                int totalCount = body.get("totalItems").asInt();
                int limit = body.get("limit").asInt();
                int newOffset = offset + limit;
                // In exceptional cases, SC-DAST may return MAXINT for limit, in which case
                // newOffset will become negative, hence we check whether newOffset > 0
                if (newOffset>0 && newOffset < totalCount) {
                    return URIHelper.addOrReplaceParam(req.getUrl(), "offset", newOffset);
                }
            }
            return null;
        };
    }
}
