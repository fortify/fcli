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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;
import lombok.SneakyThrows;

public class PagingHelper {
    public static final PagedList<JsonNode> pagedRequest(HttpRequest<?> request, INextPageUrlProducer nextPageUrlProducer) {
        return pagedRequest(request, nextPageUrlProducer, JsonNode.class);
    }
    
    @SuppressWarnings("unchecked") // TODO Can we get rid of these warnings in a better way?
    public static final <R extends JsonNode> PagedList<R> pagedRequest(HttpRequest<?> request, INextPageUrlProducer nextPageUrlProducer, Class<R> returnType) {
        return request.asPaged(r->r.asObject(returnType), nextPageUrlProducer::getNextPageUrl);
    }
    
    @SneakyThrows
    public static final String addOrReplaceParam(String uriString, String param, Object newValue) {
        return addOrReplaceParam(new URI(uriString), param, newValue).toString();
    }
    
    @SneakyThrows
    public static final URI addOrReplaceParam(URI uri, String param, Object newValue) {
        var pattern = String.format("([&?])(%s=)([^&]*)", param);
        var query = uri.getQuery();
        if (StringUtils.isNotBlank(query)) { query = query.replaceAll(pattern, ""); }
        var newParamAndValue = String.format("%s=%s", param, URLEncoder.encode(newValue.toString(), StandardCharsets.UTF_8));
        query = (StringUtils.isBlank(query) ? "" : query+"&") + newParamAndValue;
        return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), 
                uri.getPath(), query, uri.getFragment());
    }
}
