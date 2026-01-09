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
package com.fortify.cli.common.action.helper.ci;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.ci.github.GitHubEnvironment;
import com.fortify.cli.common.rest.ci.github.GitHubRestHelper;
import com.fortify.cli.common.rest.ci.github.GitHubUnirestInstanceSupplier;

import lombok.RequiredArgsConstructor;

/**
 * Action-friendly GitHub helper providing convenient methods for CI/CD workflows.
 * Automatically detects GitHub Actions environment and provides both high-level
 * convenience methods and access to underlying REST helper for advanced use cases.
 * 
 * This class is designed for use in fcli actions via the #ci.github() SpEL function.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class ActionGitHubHelper {
    private final ActionRunnerContext ctx;
    private final GitHubEnvironment env;
    private GitHubRestHelper restHelper;
    
    /**
     * Create helper with automatic environment detection.
     * Does not throw if not in CI - use getEnv() != null to check.
     */
    public ActionGitHubHelper(ActionRunnerContext ctx) {
        this.ctx = ctx;
        this.env = GitHubEnvironment.detect();
    }
    
    /**
     * Get environment data as ObjectNode for use in actions.
     * Returns null if not running in GitHub Actions.
     * Can be accessed in action YAML as: ${#ci.github().env}
     */
    public ObjectNode getEnv() {
        return env != null ? JsonHelper.getObjectMapper().valueToTree(env) : null;
    }
    
    // === SARIF Upload (Convenience Methods) ===
    
    /**
     * Upload SARIF report using detected environment values.
     * Throws exception if not in GitHub Actions or required env vars not set.
     * 
     * @param sarifContent SARIF report content as string
     * @return Response from GitHub API
     */
    public ObjectNode uploadSarif(String sarifContent) {
        requireEnv("uploadSarif");
        return getRestHelper().uploadSarif(
            env.owner(), env.repository(), env.ref(), sarifContent, env.sha());
    }
    
    /**
     * Upload SARIF report with explicit parameters.
     * 
     * @param sarifContent SARIF report content
     * @param owner Repository owner
     * @param repo Repository name
     * @param ref Git ref
     * @param commitSha Commit SHA
     * @return Response from GitHub API
     */
    public ObjectNode uploadSarif(String sarifContent, String owner, String repo, 
                                   String ref, String commitSha) {
        return getRestHelper().uploadSarif(owner, repo, ref, sarifContent, commitSha);
    }
    
    // === Pull Request Comments (Auto-Detect Context) ===
    
    /**
     * Add a general comment to the current pull request.
     * Throws exception if not in pull request context.
     * 
     * @param body Comment body (Markdown supported)
     * @return Created comment object
     */
    public ObjectNode addPrComment(String body) {
        if (!env.isPullRequest()) {
            throw new FcliSimpleException("Not running in pull request context. GITHUB_HEAD_REF is not set.");
        }
        return getRestHelper().createPullRequestComment(
            env.owner(), env.repository(), env.pullRequestNumber(), body);
    }
    
    /**
     * Add a review comment on a specific file and line in the current pull request.
     * Throws exception if not in pull request context.
     * 
     * @param path File path relative to repository root
     * @param line Line number
     * @param body Comment body (Markdown supported)
     * @return Created review comment object
     */
    public ObjectNode addReviewComment(String path, int line, String body) {
        if (!env.isPullRequest()) {
            throw new FcliSimpleException("Not running in pull request context. GITHUB_HEAD_REF is not set.");
        }
        return getRestHelper().createReviewComment(
            env.owner(), env.repository(), env.pullRequestNumber(), env.sha(), path, line, body);
    }
    
    /**
     * Add a review comment with explicit parameters.
     */
    public ObjectNode addReviewComment(String owner, String repo, int prNumber, 
                                        String commitId, String path, int line, String body) {
        return getRestHelper().createReviewComment(owner, repo, prNumber, commitId, path, line, body);
    }
    
    // === REST Helper Access ===
    
    private GitHubRestHelper getRestHelper() {
        if (restHelper == null) {
            var supplier = GitHubUnirestInstanceSupplier.builder(ctx.getUnirestContext()).build();
            restHelper = new GitHubRestHelper(supplier);
        }
        return restHelper;
    }
    
    private void requireEnv(String operation) {
        if (env == null) {
            throw new FcliSimpleException(
                "Operation '" + operation + "' requires GitHub Actions environment. " +
                "Set GITHUB_REPOSITORY and related environment variables, or check " +
                "${#ci.github().env != null} before calling.");
        }
    }
}
