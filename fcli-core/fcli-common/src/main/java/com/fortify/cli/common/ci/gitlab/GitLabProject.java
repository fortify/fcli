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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Project-scoped GitLab REST API operations. This class provides methods
 * for interacting with a specific GitLab project including security reports,
 * merge requests, branches, and commits.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class GitLabProject {
    private final UnirestInstance unirest;
    private final String projectId;
    
    // === Security Report Upload (Ultimate/Premium Tier) ===
    
    /**
     * Upload security report to GitLab (requires Ultimate/Premium tier).
     * Supports SAST, DAST, dependency scanning, container scanning, etc.
     * 
     * TODO: Similar to GitHub's GhasUnavailableException, consider adding specific
     * exception handling for when GitLab Ultimate/Premium tier features are not available.
     * This would require identifying the specific HTTP error codes and messages
     * returned by GitLab when the feature is unavailable on Free/Starter tiers.
     * 
     * Report schemas: https://docs.gitlab.com/ee/development/integrations/secure.html
     * SAST schema: https://gitlab.com/gitlab-org/security-products/security-report-schemas/-/blob/master/dist/sast-report-format.json
     * 
     * @param pipelineId Pipeline ID
     * @param reportType Report type (sast, dast, dependency_scanning, container_scanning, etc.)
     * @param reportContent Report content (JSON format matching schema)
     * @return Response from GitLab API
     */
    public ObjectNode uploadSecurityReport(String pipelineId, String reportType, String reportContent) {
        return unirest
            .post("/projects/{id}/pipelines/{pipeline_id}/security_report_summary")
            .routeParam("id", projectId)
            .routeParam("pipeline_id", pipelineId)
            .queryString("report_type", reportType)
            .header("Content-Type", "application/json")
            .body(reportContent)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Upload code quality report to GitLab merge request (available on all tiers including free).
     * Quality reports can be uploaded to merge requests to show code quality degradation.
     * 
     * Code quality format: https://docs.gitlab.com/ee/ci/testing/code_quality.html#implement-a-custom-tool
     * API endpoint: https://docs.gitlab.com/ee/api/merge_requests.html#create-merge-request-quality-report
     * 
     * Format: JSON array of objects with:
     * - description: Issue description
     * - check_name: Name of the check
     * - fingerprint: Unique identifier (MD5 hash recommended)
     * - severity: info, minor, major, critical, blocker
     * - location: { path, lines: { begin }, positions: { begin: { line, column } } }
     * 
     * Note: This requires a merge request context. For pipeline-level quality reports,
     * use artifacts instead (declare in .gitlab-ci.yml under artifacts.reports.codequality).
     * 
     * @param mergeRequestIid Merge request IID (internal ID)
     * @param reportContent Code quality report content (JSON array format)
     * @return Response from GitLab API
     */
    public ObjectNode uploadCodeQualityReport(String mergeRequestIid, String reportContent) {
        try {
            var report = JsonHelper.getObjectMapper().readTree(reportContent);
            if (!report.isArray()) {
                throw new IllegalArgumentException("Code quality report must be a JSON array");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON in code quality report: " + e.getMessage(), e);
        }
        
        return unirest
            .post("/projects/{id}/merge_requests/{merge_request_iid}/code_quality_reports")
            .routeParam("id", projectId)
            .routeParam("merge_request_iid", mergeRequestIid)
            .header("Content-Type", "application/json")
            .body(reportContent)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Merge Request Operations ===
    
    /**
     * Create a note (comment) on a merge request.
     * 
     * @param mergeRequestIid Merge request IID (internal ID)
     * @param body Comment body (Markdown supported)
     * @return Created note object
     */
    public ObjectNode createMergeRequestNote(String mergeRequestIid, String body) {
        return unirest
            .post("/projects/{id}/merge_requests/{merge_request_iid}/notes")
            .routeParam("id", projectId)
            .routeParam("merge_request_iid", mergeRequestIid)
            .body(JsonHelper.getObjectMapper().createObjectNode().put("body", body))
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Branch and Commit Operations ===
    
    /**
     * Query builder for branches.
     * 
     * @return Query builder for branches
     */
    public GitLabBranchesQuery queryBranches() {
        return new GitLabBranchesQuery();
    }
    
    /**
     * Query builder for commits.
     * 
     * @return Query builder for commits
     */
    public GitLabCommitsQuery queryCommits() {
        return new GitLabCommitsQuery();
    }
    
    /**
     * Get the latest commit for a specific branch.
     * 
     * @param branchName Branch name
     * @return ArrayNode containing the commit (single element)
     */
    public ArrayNode getLatestCommit(String branchName) {
        return unirest.get("/projects/{projectId}/repository/commits?ref_name={branchName}")
            .routeParam("projectId", projectId)
            .routeParam("branchName", branchName)
            .queryString("per_page", 1)
            .asObject(ArrayNode.class)
            .getBody();
    }
    
    // === Query Builders ===
    
    /**
     * Query builder for branches.
     */
    @Reflectable
    public class GitLabBranchesQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Search for branches by name.
         * 
         * @param search Search pattern
         * @return This query builder for chaining
         */
        public GitLabBranchesQuery search(String search) {
            return queryParam("search", search);
        }
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public GitLabBranchesQuery queryParam(String key, Object value) {
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
            var request = unirest.get("/projects/{id}/repository/branches")
                .routeParam("id", projectId);
            
            queryParams.forEach(request::queryString);
            GitLabPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
    
    /**
     * Query builder for commits.
     */
    @Reflectable
    public class GitLabCommitsQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Filter by branch name or commit SHA.
         * 
         * @param refName Branch name or commit SHA
         * @return This query builder for chaining
         */
        public GitLabCommitsQuery refName(String refName) {
            return queryParam("ref_name", refName);
        }
        
        /**
         * Filter by date - only commits after this date.
         * 
         * @param since ISO 8601 timestamp (e.g., 2024-01-01T00:00:00Z)
         * @return This query builder for chaining
         */
        public GitLabCommitsQuery since(String since) {
            return queryParam("since", since);
        }
        
        /**
         * Filter by date - only commits before this date.
         * 
         * @param until ISO 8601 timestamp
         * @return This query builder for chaining
         */
        public GitLabCommitsQuery until(String until) {
            return queryParam("until", until);
        }
        
        /**
         * Filter by file path.
         * 
         * @param path File path to filter commits
         * @return This query builder for chaining
         */
        public GitLabCommitsQuery path(String path) {
            return queryParam("path", path);
        }
        
        /**
         * Include commit stats.
         * 
         * @param withStats Whether to include stats
         * @return This query builder for chaining
         */
        public GitLabCommitsQuery withStats(boolean withStats) {
            return queryParam("with_stats", withStats);
        }
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public GitLabCommitsQuery queryParam(String key, Object value) {
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
            var request = unirest.get("/projects/{id}/repository/commits")
                .routeParam("id", projectId);
            
            queryParams.forEach(request::queryString);
            GitLabPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
}
