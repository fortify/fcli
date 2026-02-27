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
package com.fortify.cli.common.ci.github;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.Break;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Organization-scoped GitHub REST API operations. This class provides methods
 * for interacting with a GitHub organization, including listing repositories.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class GitHubOrg {
    private final UnirestInstance unirest;
    private final String owner;
    
    /**
     * Query builder for repositories.
     * 
     * @return Query builder for repositories
     */
    public GitHubRepositoriesQuery queryRepositories() {
        return new GitHubRepositoriesQuery();
    }
    
    // === Query Builders ===
    
    /**
     * Query builder for repositories.
     */
    @Reflectable
    public class GitHubRepositoriesQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Filter by repository type.
         * 
         * @param type Repository type: all, public, private, forks, sources, member
         * @return This query builder for chaining
         */
        public GitHubRepositoriesQuery type(String type) {
            return queryParam("type", type);
        }
        
        /**
         * Sort repositories.
         * 
         * @param sort Sort by: created, updated, pushed, full_name
         * @return This query builder for chaining
         */
        public GitHubRepositoriesQuery sort(String sort) {
            return queryParam("sort", sort);
        }
        
        /**
         * Sort direction.
         * 
         * @param direction Direction: asc or desc
         * @return This query builder for chaining
         */
        public GitHubRepositoriesQuery direction(String direction) {
            return queryParam("direction", direction);
        }
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public GitHubRepositoriesQuery queryParam(String key, Object value) {
            if (value != null) {
                queryParams.put(key, value);
            }
            return this;
        }
        
        /**
         * Execute the query and process results.
         * 
         * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
         */
        public void process(Function<JsonNode, Break> processor) {
            var request = unirest.get("/orgs/{owner}/repos?per_page=100")
                .routeParam("owner", owner);
            
            queryParams.forEach(request::queryString);
            GitHubPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
}
