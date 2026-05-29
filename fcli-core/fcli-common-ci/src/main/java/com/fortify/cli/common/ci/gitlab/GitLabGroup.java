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
package com.fortify.cli.common.ci.gitlab;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.Break;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Group-scoped GitLab REST API operations. This class provides methods
 * for interacting with a GitLab group, including listing projects.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class GitLabGroup {
    private final UnirestInstance unirest;
    private final String groupId;
    
    /**
     * Query builder for projects.
     * 
     * @return Query builder for projects
     */
    public GitLabProjectsQuery queryProjects() {
        return new GitLabProjectsQuery();
    }
    
    // === Query Builders ===
    
    /**
     * Query builder for projects.
     */
    @Reflectable
    public class GitLabProjectsQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Include projects from subgroups.
         * 
         * @param includeSubgroups Whether to include subgroup projects
         * @return This query builder for chaining
         */
        public GitLabProjectsQuery includeSubgroups(boolean includeSubgroups) {
            return queryParam("include_subgroups", includeSubgroups);
        }
        
        /**
         * Filter by archived status.
         * 
         * @param archived Filter archived projects
         * @return This query builder for chaining
         */
        public GitLabProjectsQuery archived(boolean archived) {
            return queryParam("archived", archived);
        }
        
        /**
         * Filter by visibility level.
         * 
         * @param visibility Visibility level: public, internal, private
         * @return This query builder for chaining
         */
        public GitLabProjectsQuery visibility(String visibility) {
            return queryParam("visibility", visibility);
        }
        
        /**
         * Search for projects.
         * 
         * @param search Search pattern
         * @return This query builder for chaining
         */
        public GitLabProjectsQuery search(String search) {
            return queryParam("search", search);
        }
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public GitLabProjectsQuery queryParam(String key, Object value) {
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
            var request = unirest.get("/groups/{id}/projects")
                .routeParam("id", groupId);
            
            queryParams.forEach(request::queryString);
            GitLabPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
}
