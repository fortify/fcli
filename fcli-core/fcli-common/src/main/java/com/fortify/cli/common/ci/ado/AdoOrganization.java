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
package com.fortify.cli.common.ci.ado;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.Break;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Organization-scoped Azure DevOps REST API operations. This class provides methods
 * for interacting with an Azure DevOps organization, including listing projects.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class AdoOrganization {
    private final UnirestInstance unirest;
    private final String organization;
    
    /**
     * Query builder for projects.
     * 
     * @return Query builder for projects
     */
    public AdoProjectsQuery queryProjects() {
        return new AdoProjectsQuery();
    }
    
    // === Query Builders ===
    
    /**
     * Query builder for projects.
     */
    @Reflectable
    public class AdoProjectsQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Filter by project state.
         * 
         * @param stateFilter State filter: all, wellFormed, deleting, new_ (use new_ for new)
         * @return This query builder for chaining
         */
        public AdoProjectsQuery stateFilter(String stateFilter) {
            return queryParam("stateFilter", stateFilter);
        }
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public AdoProjectsQuery queryParam(String key, Object value) {
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
            var request = unirest
                .get("/_apis/projects")
                .queryString("api-version", "7.0");
            
            queryParams.forEach(request::queryString);
            AdoPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
}
