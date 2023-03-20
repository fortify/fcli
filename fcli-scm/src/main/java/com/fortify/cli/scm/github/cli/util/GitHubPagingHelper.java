package com.fortify.cli.scm.github.cli.util;

import java.util.Optional;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.PagedList;

public class GitHubPagingHelper {
    private static final Pattern linkHeaderPattern = Pattern.compile("<([^>]*)>; *rel=\"([^\"]*)\"");
    private GitHubPagingHelper() {}
    
    public static final <R extends JsonNode> PagedList<R> pagedRequest(HttpRequest<?> request, Class<R> returnType) {
        return PagingHelper.pagedRequest(request, nextPageUrlProducer(), returnType);
    }
    
    public static final INextPageUrlProducer nextPageUrlProducer() {
        return r -> {
            String linkHeader = r.getHeaders().getFirst("Link");
            Optional<String> nextLink = linkHeaderPattern.matcher(linkHeader).results()
                .filter(r1->"next".equals(r1.group(2)))
                .findFirst()
                .map(r2->r2.group(1));
            return nextLink.orElse(null);
        };
    }
}
