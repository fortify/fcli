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
 * Repository-scoped Azure DevOps REST API operations. This class provides methods
 * for interacting with a specific Azure DevOps repository including SARIF upload,
 * branches, and commits.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class AdoRepository {
    private final UnirestInstance unirest;
    private final String organization;
    private final String project;
    private final String repositoryId;
    
    /**
     * Upload SARIF report to Azure DevOps Advanced Security.
     * Requires GitHub Advanced Security for Azure DevOps license.
     * 
     * TODO: Similar to GitHub's GhasUnavailableException, consider adding specific
     * exception handling for when ADO Advanced Security is not licensed/enabled.
     * This would require identifying the specific HTTP error codes and messages
     * returned by ADO when the feature is unavailable.
     * 
     * SARIF format: https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
     * ADO Advanced Security: https://learn.microsoft.com/en-us/azure/devops/repos/security/github-advanced-security
     * 
     * @param ref Git ref (branch/tag)
     * @param commitSha Commit SHA
     * @param sarifContent SARIF report content
     * @return Response from Azure DevOps API
     */
    public ObjectNode uploadSarif(String ref, String commitSha, String sarifContent) {
        var body = JsonHelper.getObjectMapper().createObjectNode()
            .put("repository", repositoryId)
            .put("ref", ref)
            .put("commitSha", commitSha)
            .put("sarif", sarifContent);
        
        return unirest
            .post("/{organization}/{project}/_apis/alert/sarif")
            .routeParam("organization", organization)
            .routeParam("project", project)
            .queryString("api-version", "7.1-preview.1")
            .header("Content-Type", "application/json")
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Query builder for branches.
     * 
     * @return Query builder for branches
     */
    public AdoBranchesQuery queryBranches() {
        return new AdoBranchesQuery();
    }
    
    /**
     * Query builder for commits.
     * 
     * @return Query builder for commits
     */
    public AdoCommitsQuery queryCommits() {
        return new AdoCommitsQuery();
    }
    
    /**
     * Get the latest commit for a specific branch.
     * 
     * @param branchName Branch name
     * @return ObjectNode containing commit data
     */
    public ObjectNode getLatestCommit(String branchName) {
        return unirest
            .get("/{project}/_apis/git/repositories/{repositoryId}/commits")
            .routeParam("project", project)
            .routeParam("repositoryId", repositoryId)
            .queryString("searchCriteria.itemVersion.version", branchName)
            .queryString("searchCriteria.itemVersion.versionType", "branch")
            .queryString("searchCriteria.$top", "1")
            .queryString("api-version", "7.0")
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Query Builders ===
    
    /**
     * Query builder for branches.
     */
    @Reflectable
    public class AdoBranchesQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Filter branches by name.
         * 
         * @param filter Filter pattern (default: heads/)
         * @return This query builder for chaining
         */
        public AdoBranchesQuery filter(String filter) {
            return queryParam("filter", filter);
        }
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public AdoBranchesQuery queryParam(String key, Object value) {
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
                .get("/{project}/_apis/git/repositories/{repositoryId}/refs")
                .routeParam("project", project)
                .routeParam("repositoryId", repositoryId)
                .queryString("api-version", "7.0");
            
            // Default filter if not specified
            if (!queryParams.containsKey("filter")) {
                request = request.queryString("filter", "heads/");
            }
            
            queryParams.forEach(request::queryString);
            AdoPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
    
    /**
     * Query builder for commits.
     */
    @Reflectable
    public class AdoCommitsQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Filter by branch name.
         * 
         * @param branchName Branch name
         * @return This query builder for chaining
         */
        public AdoCommitsQuery branchName(String branchName) {
            queryParam("searchCriteria.itemVersion.version", branchName);
            queryParam("searchCriteria.itemVersion.versionType", "branch");
            return this;
        }
        
        /**
         * Filter by date - only commits after this date.
         * 
         * @param fromDate ISO 8601 timestamp
         * @return This query builder for chaining
         */
        public AdoCommitsQuery fromDate(String fromDate) {
            return queryParam("searchCriteria.fromDate", fromDate);
        }
        
        /**
         * Filter by date - only commits before this date.
         * 
         * @param toDate ISO 8601 timestamp
         * @return This query builder for chaining
         */
        public AdoCommitsQuery toDate(String toDate) {
            return queryParam("searchCriteria.toDate", toDate);
        }
        
        /**
         * Filter by author.
         * 
         * @param author Author name or email
         * @return This query builder for chaining
         */
        public AdoCommitsQuery author(String author) {
            return queryParam("searchCriteria.author", author);
        }
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public AdoCommitsQuery queryParam(String key, Object value) {
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
                .get("/{project}/_apis/git/repositories/{repositoryId}/commits")
                .routeParam("project", project)
                .routeParam("repositoryId", repositoryId)
                .queryString("searchCriteria.$top", "100")
                .queryString("api-version", "7.0");
            
            queryParams.forEach(request::queryString);
            AdoPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
}
