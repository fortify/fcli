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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Project-scoped Azure DevOps REST API operations. This class provides methods
 * for interacting with an Azure DevOps project including test results, pull requests,
 * and repositories.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class AdoProject {
    private final UnirestInstance unirest;
    private final String organization;
    private final String project;
    
    /**
     * Publish test results to Azure DevOps (available on all tiers).
     * While primarily for test results, this can be adapted for security findings on free tier.
     * 
     * Supported formats: JUnit, NUnit, XUnit, VSTest, CTest
     * API documentation: https://learn.microsoft.com/en-us/rest/api/azure/devops/test/results
     * 
     * For security findings, format as test failures where:
     * - Test name = vulnerability title
     * - Error message = vulnerability description  
     * - Stack trace = file path and line number
     * 
     * @param buildId Build ID
     * @param testResults Test results in specified format
     * @param testRunner Test runner type (JUnit, NUnit, XUnit, VSTest, CTest)
     * @return Response from Azure DevOps API
     */
    public ObjectNode publishTestResults(String buildId, String testResults, String testRunner) {
        var body = JsonHelper.getObjectMapper().createObjectNode()
            .put("testRunner", testRunner)
            .put("results", testResults);
        
        return unirest
            .post("/{project}/_apis/test/runs")
            .routeParam("project", project)
            .queryString("api-version", "7.0")
            .header("Content-Type", "application/json")
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Create a comment thread on a pull request.
     * 
     * @param repositoryId Repository ID
     * @param pullRequestId Pull request ID
     * @param comment Comment text
     * @return Created thread object
     */
    public ObjectNode createPullRequestThread(String repositoryId, String pullRequestId, String comment) {
        var body = JsonHelper.getObjectMapper().createObjectNode();
        var commentsArray = body.putArray("comments");
        commentsArray.addObject().put("content", comment);
        
        return unirest
            .post("/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}/threads")
            .routeParam("project", project)
            .routeParam("repositoryId", repositoryId)
            .routeParam("pullRequestId", pullRequestId)
            .queryString("api-version", "7.0")
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Query builder for repositories.
     * 
     * @return Query builder for repositories
     */
    public AdoRepositoriesQuery queryRepositories() {
        return new AdoRepositoriesQuery();
    }
    
    // === Query Builders ===
    
    /**
     * Query builder for repositories.
     */
    @Reflectable
    public class AdoRepositoriesQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public AdoRepositoriesQuery queryParam(String key, Object value) {
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
                .get("/{project}/_apis/git/repositories")
                .routeParam("project", project)
                .queryString("api-version", "7.0");
            
            queryParams.forEach(request::queryString);
            AdoPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
}
