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
package com.fortify.cli.common.rest.ci.github;

import java.util.Base64;
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
    
    // === Code Scanning / SARIF Upload ===
    
    /**
     * Upload SARIF report to GitHub Code Scanning.
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
        var compressed = gzipAndBase64(sarifContent);
        
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
        new GitHubRepositoriesProcessor(getUnirest(), owner).process(processor);
    }
    
    /**
     * Process branches for a repository.
     * 
     * @param owner Repository owner (organization or user)
     * @param repo Repository name
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processBranches(String owner, String repo, Function<JsonNode, Break> processor) {
        new GitHubBranchesProcessor(getUnirest(), owner, repo).process(processor);
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
        new GitHubCommitsProcessor(getUnirest(), owner, repo, sha, since).process(processor);
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
    
    private String gzipAndBase64(String content) {
        // TODO: Implement gzip compression and base64 encoding
        // For now, just return base64-encoded content
        return Base64.getEncoder().encodeToString(content.getBytes());
    }
    
    // === Inner Classes ===
    
    @RequiredArgsConstructor
    private static final class GitHubRepositoriesProcessor {
        private final UnirestInstance unirest;
        private final String owner;
        
        public void process(Function<JsonNode, Break> processor) {
            GitHubPagingHelper.processPagedItems(
                unirest,
                unirest.get("/orgs/{owner}/repos").routeParam("owner", owner),
                processor
            );
        }
    }    
    @RequiredArgsConstructor
    private static final class GitHubBranchesProcessor {
        private final UnirestInstance unirest;
        private final String owner;
        private final String repo;
        
        public void process(Function<JsonNode, Break> processor) {
            GitHubPagingHelper.processPagedItems(
                unirest,
                unirest.get("/repos/{owner}/{repo}/branches?per_page=100")
                    .routeParam("owner", owner)
                    .routeParam("repo", repo),
                processor
            );
        }
    }
    
    @RequiredArgsConstructor
    private static final class GitHubCommitsProcessor {
        private final UnirestInstance unirest;
        private final String owner;
        private final String repo;
        private final String sha;
        private final String since;
        
        public void process(Function<JsonNode, Break> processor) {
            var request = unirest.get("/repos/{owner}/{repo}/commits?per_page=100")
                .routeParam("owner", owner)
                .routeParam("repo", repo)
                .queryString("sha", sha);
            if (since != null) {
                request = request.queryString("since", since);
            }
            GitHubPagingHelper.processPagedItems(unirest, request, processor);
        }
    }}
