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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.helper.ci.github.GhasUnavailableException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.common.util.GzipHelper;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Repository-scoped GitHub REST API operations. This class provides methods
 * for interacting with a specific GitHub repository including code scanning,
 * check runs, pull requests, branches, and commits.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class GitHubRepo {
    private final UnirestInstance unirest;
    private final String owner;
    private final String repo;
    
    // === Code Scanning / SARIF Upload (Advanced Security) ===
    
    /**
     * Upload SARIF report to GitHub Code Scanning (requires GitHub Advanced Security).
     * 
     * Throws GhasUnavailableException if GitHub Advanced Security is not enabled for the repository
     * (detected by 403/404 status with "code-scanning" in the error message).
     * 
     * SARIF format: https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
     * GitHub SARIF support: https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning
     * API documentation: https://docs.github.com/en/rest/code-scanning
     * 
     * @param ref Git ref (e.g., refs/heads/main)
     * @param sarifContent SARIF report content as string
     * @param commitSha Commit SHA (required)
     * @return Response from GitHub API
     * @throws GhasUnavailableException if GitHub Advanced Security is not available
     */
    public ObjectNode uploadSarif(String ref, String sarifContent, String commitSha) {
        try {
            var compressed = GzipHelper.gzipAndBase64(sarifContent);
            
            var body = JsonHelper.getObjectMapper().createObjectNode()
                .put("sarif", compressed)
                .put("ref", ref)
                .put("commit_sha", commitSha);
            
            return unirest
                .post("/repos/{owner}/{repo}/code-scanning/sarifs")
                .routeParam("owner", owner)
                .routeParam("repo", repo)
                .body(body)
                .asObject(ObjectNode.class)
                .getBody();
        } catch (UnexpectedHttpResponseException e) {
            var status = e.getStatus();
            var isGhasUnavailable = (status == 403 || status == 404) && 
                                   e.getMessage().contains("code-scanning");
            if (isGhasUnavailable) {
                throw new GhasUnavailableException(
                    "GitHub Advanced Security not available for this repository. " +
                    "SARIF upload requires GHAS to be enabled.", e);
            }
            throw e; // Re-throw other HTTP exceptions
        }
    }
    
    /**
     * Create a check run for the commit (available on all tiers including free).
     * This can report scan results without requiring GitHub Advanced Security.
     * 
     * The implementation handles annotations automatically:
     * 1. Creates check run with status "in_progress" (without conclusion/annotations)
     * 2. If annotations present, updates check run with annotations (paginated in batches of 50)
     * 3. Updates check run with final status and conclusion
     * 
     * Expected body format matches GitHub REST API (https://docs.github.com/en/rest/checks/runs):
     * - name: Check run name (required)
     * - head_sha: Commit SHA (required)
     * - status: queued, in_progress, or completed (required)
     * - conclusion: success, failure, neutral, etc. (optional, set to finalize check run)
     * - started_at: ISO 8601 timestamp (optional, defaults to current time)
     * - completed_at: ISO 8601 timestamp (optional, set when status=completed)
     * - output: Object with title, summary, text, and annotations (optional)
     *   - annotations: Array of annotation objects (optional, auto-paginated if >50)
     * 
     * @param body Full check run request body in GitHub API format
     * @return Response from GitHub API (from initial create request)
     */
    public ObjectNode createCheckRun(ObjectNode body) {
        var originalStatus = body.path("status").asText(null);
        var originalConclusion = body.path("conclusion").asText(null);
        var annotations = extractAnnotations(body);
        
        var createBody = prepareInitialCreateBody(body);
        var response = unirest
            .post("/repos/{owner}/{repo}/check-runs")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .body(createBody)
            .asObject(ObjectNode.class)
            .getBody();
        
        var checkRunId = response.get("id").asLong();
        
        if (annotations != null && annotations.size() > 0) {
            addCheckRunAnnotations(checkRunId, annotations, createBody);
        }
        
        boolean needsFinalUpdate = !"in_progress".equals(originalStatus) || originalConclusion != null;
        if (needsFinalUpdate) {
            finalizeCheckRun(checkRunId, originalStatus, originalConclusion, body.has("completed_at"));
        }
        
        return response;
    }
    
    /**
     * Extract annotations from check run body.
     * 
     * @param body Check run body
     * @return Annotations array or null if not present
     */
    private ArrayNode extractAnnotations(ObjectNode body) {
        var annotations = (ArrayNode) body.path("output").path("annotations");
        return annotations.isMissingNode() ? null : annotations;
    }
    
    /**
     * Prepare initial check run body with in_progress status and without annotations.
     * 
     * @param body Original check run body
     * @return Modified body for initial create
     */
    private ObjectNode prepareInitialCreateBody(ObjectNode body) {
        var createBody = body.deepCopy();
        createBody.put("status", "in_progress");
        createBody.remove("conclusion");
        if (createBody.has("output")) {
            ((ObjectNode) createBody.get("output")).remove("annotations");
        }
        if (!createBody.has("started_at")) {
            createBody.put("started_at", java.time.Instant.now().toString());
        }
        return createBody;
    }
    
    /**
     * Add annotations to check run in batches of 50.
     * 
     * @param checkRunId Check run ID
     * @param annotations All annotations to add
     * @param initialCreateBody Body used for initial create (contains output with title/summary)
     */
    private void addCheckRunAnnotations(long checkRunId, ArrayNode annotations, ObjectNode initialCreateBody) {
        for (int offset = 0; offset < annotations.size(); offset += 50) {
            var batch = JsonHelper.getObjectMapper().createArrayNode();
            int batchEnd = Math.min(offset + 50, annotations.size());
            for (int i = offset; i < batchEnd; i++) {
                batch.add(annotations.get(i));
            }
            updateCheckRunAnnotations(checkRunId, batch, initialCreateBody);
        }
    }
    
    /**
     * Finalize check run with final status and conclusion.
     * 
     * @param checkRunId Check run ID
     * @param status Final status
     * @param conclusion Final conclusion (may be null)
     * @param hasCompletedAt Whether body already has completed_at
     */
    private void finalizeCheckRun(long checkRunId, String status, String conclusion, boolean hasCompletedAt) {
        var updateBody = JsonHelper.getObjectMapper().createObjectNode();
        updateBody.put("status", status);
        if (conclusion != null) {
            updateBody.put("conclusion", conclusion);
        }
        if ("completed".equals(status) && !hasCompletedAt) {
            updateBody.put("completed_at", java.time.Instant.now().toString());
        }
        updateCheckRun(checkRunId, updateBody);
    }
    
    /**
     * Update check run with annotation batch.
     * 
     * @param checkRunId Check run ID
     * @param annotations Batch of annotations (max 50)
     * @param initialCreateBody Body used for initial create (output reused here)
     */
    private void updateCheckRunAnnotations(long checkRunId, ArrayNode annotations, ObjectNode initialCreateBody) {
        var updateBody = JsonHelper.getObjectMapper().createObjectNode();
        if (initialCreateBody.has("output")) {
            updateBody.set("output", initialCreateBody.get("output").deepCopy());
            ((ObjectNode) updateBody.get("output")).set("annotations", annotations);
        } else {
            var output = updateBody.putObject("output");
            output.set("annotations", annotations);
        }
        updateCheckRun(checkRunId, updateBody);
    }
    
    /**
     * Update check run via PATCH request.
     * 
     * @param checkRunId Check run ID
     * @param body Update body
     * @return Response from GitHub API
     */
    private ObjectNode updateCheckRun(long checkRunId, ObjectNode body) {
        return unirest
            .patch("/repos/{owner}/{repo}/check-runs/{check_run_id}")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .routeParam("check_run_id", String.valueOf(checkRunId))
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Pull Request Operations ===
    
    /**
     * Create a general comment on a pull request (issue comment).
     * 
     * @param pullNumber Pull request number
     * @param body Comment body (Markdown supported)
     * @return Created comment object
     */
    public ObjectNode createPullRequestComment(String pullNumber, String body) {
        return unirest
            .post("/repos/{owner}/{repo}/issues/{issue_number}/comments")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .routeParam("issue_number", pullNumber)
            .body(JsonHelper.getObjectMapper().createObjectNode().put("body", body))
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Create a review comment on a specific line in a pull request.
     * 
     * @param pullNumber Pull request number
     * @param commitId Commit SHA to comment on
     * @param path File path in repository
     * @param line Line number to comment on
     * @param body Comment body (Markdown supported)
     * @return Created review comment object
     */
    public ObjectNode createReviewComment(String pullNumber, String commitId, 
                                           String path, int line, String body) {
        var requestBody = JsonHelper.getObjectMapper().createObjectNode()
            .put("body", body)
            .put("commit_id", commitId)
            .put("path", path)
            .put("line", line);
        
        return unirest
            .post("/repos/{owner}/{repo}/pulls/{pull_number}/comments")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .routeParam("pull_number", pullNumber)
            .body(requestBody)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Branch and Commit Operations ===
    
    /**
     * Query builder for branches.
     * 
     * @return Query builder for branches
     */
    public GitHubBranchesQuery queryBranches() {
        return new GitHubBranchesQuery();
    }
    
    /**
     * Query builder for commits.
     * 
     * @return Query builder for commits
     */
    public GitHubCommitsQuery queryCommits() {
        return new GitHubCommitsQuery();
    }
    
    /**
     * Get the latest commit for a specific branch.
     * 
     * @param sha Branch SHA to get commit for
     * @return ArrayNode containing the commit (single element)
     */
    public ArrayNode getLatestCommit(String sha) {
        return unirest.get("/repos/{owner}/{repo}/commits")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .queryString("sha", sha)
            .queryString("per_page", 1)
            .asObject(ArrayNode.class)
            .getBody();
    }
    
    // === Query Builders ===
    
    /**
     * Query builder for branches.
     */
    @Reflectable
    public class GitHubBranchesQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public GitHubBranchesQuery queryParam(String key, Object value) {
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
            var request = unirest.get("/repos/{owner}/{repo}/branches?per_page=100")
                .routeParam("owner", owner)
                .routeParam("repo", repo);
            
            queryParams.forEach(request::queryString);
            GitHubPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
    
    /**
     * Query builder for commits.
     */
    @Reflectable
    public class GitHubCommitsQuery {
        private final Map<String, Object> queryParams = new HashMap<>();
        
        /**
         * Filter by branch name or commit SHA.
         * 
         * @param sha Branch name or commit SHA to start from
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery sha(String sha) {
            return queryParam("sha", sha);
        }
        
        /**
         * Filter by date - only commits after this date.
         * 
         * @param since ISO 8601 timestamp (e.g., 2024-01-01T00:00:00Z)
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery since(String since) {
            return queryParam("since", since);
        }
        
        /**
         * Filter by date - only commits before this date.
         * 
         * @param until ISO 8601 timestamp
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery until(String until) {
            return queryParam("until", until);
        }
        
        /**
         * Filter by author.
         * 
         * @param author GitHub login or email address
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery author(String author) {
            return queryParam("author", author);
        }
        
        /**
         * Filter by file path.
         * 
         * @param path File path to filter commits
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery path(String path) {
            return queryParam("path", path);
        }
        
        /**
         * Add a custom query parameter.
         * 
         * @param key Parameter name
         * @param value Parameter value (null values are ignored)
         * @return This query builder for chaining
         */
        public GitHubCommitsQuery queryParam(String key, Object value) {
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
            var request = unirest.get("/repos/{owner}/{repo}/commits?per_page=100")
                .routeParam("owner", owner)
                .routeParam("repo", repo);
            
            queryParams.forEach(request::queryString);
            GitHubPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
}
