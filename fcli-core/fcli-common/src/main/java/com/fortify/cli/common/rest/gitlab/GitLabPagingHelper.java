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
package com.fortify.cli.common.rest.gitlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.LinkHeaderNextPageUrlProducerFactory;
import com.fortify.cli.common.rest.paging.PagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

/**
 * This class provides utility methods for handling GitLab paging.
 * 
 * @author rsenden
 */
public class GitLabPagingHelper {
    private GitLabPagingHelper() {}
    
    public static final <R extends JsonNode> PagedList<R> pagedRequest(HttpRequest<?> request, Class<R> returnType) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer(), returnType);
    }
    
    public static final INextPageUrlProducer nextPageUrlProducer() {
        return LinkHeaderNextPageUrlProducerFactory.nextPageUrlProducer("link", "next");
    }
}
