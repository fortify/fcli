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

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;
import com.fortify.cli.common.util.GzipHelper;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Generic GitHub REST API helper providing core operations for repositories,
 * pull requests, code scanning, and other GitHub features. This class can be
 * used from commands, actions, and other modules like fcli-license.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class GitHubRestHelper {
    private final GitHubUnirestInstanceSupplier unirestInstanceSupplier;
    
    // === Code Scanning / SARIF Upload (Advanced Security) ===
    
    /**
     * Upload SARIF report to GitHub Code Scanning (requires GitHub Advanced Security).
     * 
     * SARIF format: https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
     * GitHub SARIF support: https://docs.github.com/en/code-security/code-scanning/integrating-with-code-scanning/sarif-support-for-code-scanning
     * API documentation: https://docs.github.com/en/rest/code-scanning
     * 
     * @param owner Repository owner
     * @param repo Repository name
     * @param ref Git ref (e.g., refs/heads/main)
     * @param sarifContent SARIF report content as string
     * @param commitSha Commit SHA (required)
     * @return Response from GitHub API
     */
    public ObjectNode uploadSarif(String owner, String repo, String ref, 
                                   String sarifContent, String commitSha) {
        // GitHub requires SARIF content to be gzip-compressed and base64-encoded
        var compressed = GzipHelper.gzipAndBase64(sarifContent);
        
        var body = JsonHelper.getObjectMapper().createObjectNode()
            .put("sarif", compressed)
            .put("ref", ref)
            .put("commit_sha", commitSha);
        
        return getUnirest()
            .post("/repos/{owner}/{repo}/code-scanning/sarifs")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Create a check run for the commit (available on all tiers including free).
     * This can report scan results without requiring GitHub Advanced Security.
     * For file/line-level findings, use createCheckRunWithAnnotations instead.
     * 
     * API documentation: https://docs.github.com/en/rest/checks/runs
     * 
     * @param owner Repository owner
     * @param repo Repository name
     * @param name Check run name
     * @param headSha Commit SHA
     * @param status Status (queued, in_progress, completed)
     * @param conclusion Conclusion (success, failure, neutral, cancelled, skipped, timed_out, action_required) - required if status=completed
     * @param title Summary title
     * @param summary Summary text (Markdown supported, max 65535 chars)
     * @return Response from GitHub API
     */
    public ObjectNode createCheckRun(String owner, String repo, String name,
                                      String headSha, String status, String conclusion,
                                      String title, String summary) {
        return createCheckRunWithAnnotations(owner, repo, name, headSha, status, conclusion, title, summary, null);
    }
    
    /**
     * Create a check run with annotations for file/line-level details (available on all tiers).
     * Annotations allow displaying vulnerability details at specific file locations.
     * Automatically handles pagination for more than 50 annotations by creating the check run
     * with the first 50, then updating with remaining annotations in batches of 50.
     * 
     * Annotation format:
     * - path: File path relative to repository root
     * - start_line: Starting line number (required)
     * - end_line: Ending line number (defaults to start_line)
     * - annotation_level: notice, warning, or failure
     * - message: Annotation message (Markdown supported)
     * - title: Optional short title
     * 
     * API documentation: https://docs.github.com/en/rest/checks/runs#create-a-check-run
     * 
     * @param owner Repository owner
     * @param repo Repository name
     * @param name Check run name
     * @param headSha Commit SHA
     * @param status Status (queued, in_progress, completed)
     * @param conclusion Conclusion (required if status=completed)
     * @param title Summary title
     * @param summary Summary text (Markdown supported)
     * @param annotations Array of annotation objects (automatic pagination for >50)
     * @return Response from GitHub API (from initial create request)
     */
    public ObjectNode createCheckRunWithAnnotations(String owner, String repo, String name,
                                                     String headSha, String status, String conclusion,
                                                     String title, String summary, ArrayNode annotations) {
        // Prepare first batch (up to 50 annotations)
        ArrayNode firstBatch = null;
        if (annotations != null && annotations.size() > 0) {
            firstBatch = JsonHelper.getObjectMapper().createArrayNode();
            int batchSize = Math.min(50, annotations.size());
            for (int i = 0; i < batchSize; i++) {
                firstBatch.add(annotations.get(i));
            }
        }
        
        // Create check run with first batch
        var body = JsonHelper.getObjectMapper().createObjectNode()
            .put("name", name)
            .put("head_sha", headSha)
            .put("status", status);
        
        if (conclusion != null) {
            body.put("conclusion", conclusion);
        }
        
        if (title != null || summary != null || firstBatch != null) {
            var output = body.putObject("output");
            if (title != null) output.put("title", title);
            if (summary != null) output.put("summary", summary);
            if (firstBatch != null) {
                output.set("annotations", firstBatch);
            }
        }
        
        var response = getUnirest()
            .post("/repos/{owner}/{repo}/check-runs")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
        
        // If more than 50 annotations, update check run with remaining batches
        if (annotations != null && annotations.size() > 50) {
            var checkRunId = response.get("id").asLong();
            for (int offset = 50; offset < annotations.size(); offset += 50) {
                var batch = JsonHelper.getObjectMapper().createArrayNode();
                int batchEnd = Math.min(offset + 50, annotations.size());
                for (int i = offset; i < batchEnd; i++) {
                    batch.add(annotations.get(i));
                }
                updateCheckRunAnnotations(owner, repo, checkRunId, batch);
            }
        }
        
        return response;
    }
    
    /**
     * Update check run with additional annotations.
     * Used internally by createCheckRunWithAnnotations for pagination.
     * 
     * @param owner Repository owner
     * @param repo Repository name
     * @param checkRunId Check run ID
     * @param annotations Array of annotation objects (max 50)
     */
    private void updateCheckRunAnnotations(String owner, String repo, long checkRunId, ArrayNode annotations) {
        var body = JsonHelper.getObjectMapper().createObjectNode();
        var output = body.putObject("output");
        output.set("annotations", annotations);
        
        getUnirest()
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
     * @param owner Repository owner
     * @param repo Repository name
     * @param pullNumber Pull request number
     * @param body Comment body (Markdown supported)
     * @return Created comment object
     */
    public ObjectNode createPullRequestComment(String owner, String repo, 
                                                int pullNumber, String body) {
        return getUnirest()
            .post("/repos/{owner}/{repo}/issues/{issue_number}/comments")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .routeParam("issue_number", String.valueOf(pullNumber))
            .body(JsonHelper.getObjectMapper().createObjectNode().put("body", body))
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Create a review comment on a specific line in a pull request.
     * 
     * @param owner Repository owner
     * @param repo Repository name
     * @param pullNumber Pull request number
     * @param commitId Commit SHA to comment on
     * @param path File path in repository
     * @param line Line number to comment on
     * @param body Comment body (Markdown supported)
     * @return Created review comment object
     */
    public ObjectNode createReviewComment(String owner, String repo, int pullNumber,
                                           String commitId, String path, int line, String body) {
        var requestBody = JsonHelper.getObjectMapper().createObjectNode()
            .put("body", body)
            .put("commit_id", commitId)
            .put("path", path)
            .put("line", line);
        
        return getUnirest()
            .post("/repos/{owner}/{repo}/pulls/{pull_number}/comments")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .routeParam("pull_number", String.valueOf(pullNumber))
            .body(requestBody)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Repository Operations ===

    /**
     * Process all repositories for an organization/user.
     * 
     * @param owner Organization or user name
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processRepositories(String owner, Function<JsonNode, Break> processor) {
        GitHubPagingHelper.processPagedItems(
            getUnirest(),
            getUnirest().get("/orgs/{owner}/repos").routeParam("owner", owner),
            processor
        );
    }
    
    /**
     * Process branches for a repository.
     * 
     * @param owner Repository owner (organization or user)
     * @param repo Repository name
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processBranches(String owner, String repo, Function<JsonNode, Break> processor) {
        GitHubPagingHelper.processPagedItems(
            getUnirest(),
            getUnirest().get("/repos/{owner}/{repo}/branches?per_page=100")
                .routeParam("owner", owner)
                .routeParam("repo", repo),
            processor
        );
    }
    

    /**
     * Process commits for a repository.
     * 
     * @param owner Repository owner (organization or user)
     * @param repo Repository name
     * @param sha Branch name or commit SHA to start from
     * @param since ISO 8601 timestamp to filter commits after this date (optional)
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processCommits(String owner, String repo, String sha, String since, Function<JsonNode, Break> processor) {
        var request = getUnirest().get("/repos/{owner}/{repo}/commits?per_page=100")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .queryString("sha", sha);
        if (since != null) {
            request = request.queryString("since", since);
        }
        GitHubPagingHelper.processPagedItems(getUnirest(), request, processor);
    }
    
    /**
     * Get the latest commit for a specific branch.
     * 
     * @param owner Repository owner (organization or user)
     * @param repo Repository name
     * @param sha Branch SHA to get commit for
     * @return ArrayNode containing the commit (single element)
     */
    public ArrayNode getLatestCommit(String owner, String repo, String sha) {
        return getUnirest().get("/repos/{owner}/{repo}/commits")
            .routeParam("owner", owner)
            .routeParam("repo", repo)
            .queryString("sha", sha)
            .queryString("per_page", 1)
            .asObject(ArrayNode.class)
            .getBody();
    }
        // === Internal Methods ===
    
    /**
     * Get the UnirestInstance from the supplier.
     */
    private UnirestInstance getUnirest() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
}
