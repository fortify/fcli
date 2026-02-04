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

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.ci.github.GitHubEnvironment;
import com.fortify.cli.common.ci.github.GitHubRestHelper;
import com.fortify.cli.common.ci.github.GitHubUnirestInstanceSupplier;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

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
@SpelFunctionPrefix("github.")
public class ActionGitHubSpelFunctions implements IActionSpelFunctions {
    private final ActionRunnerContext ctx;
    private final GitHubEnvironment env;
    private GitHubRestHelper restHelper;
    
    /**
     * Create helper with automatic environment detection.
     * Does not throw if not in CI - use getEnv() != null to check.
     */
    public ActionGitHubSpelFunctions(ActionRunnerContext ctx) {
        this.ctx = ctx;
        this.env = GitHubEnvironment.detect();
    }
    
    /**
     * Get environment data as ObjectNode for use in actions.
     * Returns null if not running in GitHub Actions.
     * Can be accessed in action YAML as: ${#ci.github().env}
     */
    @SpelFunction(cat=ci, desc="Returns GitHub Actions environment data as ObjectNode (auto-detected for the current workflow run)",
            returns="Environment data or `null` if not running in GitHub Actions",
            returnType=GitHubEnvironment.class)
    @Override
    public ObjectNode getEnv() {
        return env != null ? JsonHelper.getObjectMapper().valueToTree(env) : null;
    }
    
    /**
     * Returns "github" as the CI system type.
     */
    @SpelFunction(cat=ci, desc="Returns CI system type identifier",
            returns="\"github\"")
    @Override
    public String getType() {
        return GitHubEnvironment.TYPE;
    }
    
    // === SARIF Upload (Advanced Security - Paid Tier) ===
    
    /**
     * Upload SARIF report using detected environment values.
     * Requires GitHub Advanced Security (GHAS).
     * 
     * SARIF format: https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
     * 
     * @param sarifContent SARIF report content as string
     * @return Response from GitHub API
     */
    @SpelFunction(cat=ci, desc="Uploads SARIF to GitHub Code Scanning (paid tier, requires GHAS) using repository/ref/commit from the current workflow run",
            returns="Response from GitHub API")
    public ObjectNode uploadSarif(
            @SpelFunctionParam(name="sarifContent", desc="SARIF report content as string") String sarifContent) {
        requireEnv("uploadSarif");
        var repoName = env.ciRepository().name();
        var owner = repoName.full().contains("/") ? repoName.full().split("/")[0] : "";
        var repo = repoName.short_();
        var ref = env.ciBranch().full();
        var sha = env.ciCommit().id().full();
        return getRestHelper().uploadSarif(owner, repo, ref, sarifContent, sha);
    }
    
    // === Check Runs (Free Tier Alternative with file/line details) ===
    
    /**
     * Create a check run using detected environment values.
     * This can report scan results without requiring GitHub Advanced Security.
     * For detailed vulnerability reports, use createCheckRunWithAnnotations instead.
     * 
     * @param name Check run name
     * @param status Status (queued, in_progress, completed)
     * @param conclusion Conclusion (success, failure, neutral, cancelled, skipped, timed_out, action_required)
     * @param title Summary title
     * @param summary Summary text (Markdown supported, max 65535 chars)
     * @return Response from GitHub API
     */
    @SpelFunction(cat=ci, desc="Creates check run (free tier, for summary only) using repository and commit detected from the current run",
            returns="Response from GitHub API")
    public ObjectNode createCheckRun(
            @SpelFunctionParam(name="name", desc="check run name") String name,
            @SpelFunctionParam(name="status", desc="status: queued, in_progress, completed") String status,
            @SpelFunctionParam(name="conclusion", desc="conclusion: success, failure, neutral, cancelled, skipped, timed_out, action_required") String conclusion,
            @SpelFunctionParam(name="title", desc="summary title") String title,
            @SpelFunctionParam(name="summary", desc="summary text in Markdown (max 65535 chars)") String summary) {
        requireEnv("createCheckRun");
        var repoName = env.ciRepository().name();
        var owner = repoName.full().contains("/") ? repoName.full().split("/")[0] : "";
        var repo = repoName.short_();
        var sha = env.ciCommit().id().full();
        return getRestHelper().createCheckRun(owner, repo, name, sha, status, conclusion, title, summary);
    }
    
    /**
     * Create a check run with annotations for file/line-level vulnerability details.
     * Annotations show findings at specific file locations.
     * Automatically handles pagination - no limit on number of annotations.
     * 
     * Annotation format (JSON array):
     * [{
     *   "path": "src/Main.java",
     *   "start_line": 42,
     *   "end_line": 42,
     *   "annotation_level": "warning",
     *   "message": "SQL Injection vulnerability detected",
     *   "title": "SQL Injection"
     * }]
     * 
     * @param name Check run name
     * @param status Status (queued, in_progress, completed)
     * @param conclusion Conclusion
     * @param title Summary title
     * @param summary Summary text (Markdown)
     * @param annotations Array of annotation objects (automatic pagination for large sets)
     * @return Response from GitHub API
     */
    @SpelFunction(cat=ci, desc="Creates check run with file/line annotations (free tier, shows vulnerabilities at source locations, auto-paginates) using the detected repository/commit",
            returns="Response from GitHub API")
    public ObjectNode createCheckRunWithAnnotations(
            @SpelFunctionParam(name="name", desc="check run name") String name,
            @SpelFunctionParam(name="status", desc="status: queued, in_progress, completed") String status,
            @SpelFunctionParam(name="conclusion", desc="conclusion: success, failure, neutral, etc.") String conclusion,
            @SpelFunctionParam(name="title", desc="summary title") String title,
            @SpelFunctionParam(name="summary", desc="summary text in Markdown") String summary,
            @SpelFunctionParam(name="annotations", desc="array of annotation objects with path, line, level, message (auto-paginated)") ArrayNode annotations) {
        requireEnv("createCheckRunWithAnnotations");
        var repoName = env.ciRepository().name();
        var owner = repoName.full().contains("/") ? repoName.full().split("/")[0] : "";
        var repo = repoName.short_();
        var sha = env.ciCommit().id().full();
        return getRestHelper().createCheckRunWithAnnotations(owner, repo, name, sha, status, conclusion, title, summary, annotations);
    }
    
    // === Pull Request Comments (Auto-Detect Context) ===
    
    /**
     * Add a general comment to the current pull request.
     * Throws exception if not in pull request context.
     * 
     * @param body Comment body (Markdown supported)
     * @return Created comment object
     */
    @SpelFunction(cat=ci, desc="Adds a comment to the current pull request detected from the workflow run",
            returns="Created comment object")
    public ObjectNode addPrComment(
            @SpelFunctionParam(name="body", desc="comment body (Markdown supported)") String body) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in pull request context. GITHUB_HEAD_REF is not set.");
        }
        var repoName = env.ciRepository().name();
        var owner = repoName.full().contains("/") ? repoName.full().split("/")[0] : "";
        var repo = repoName.short_();
        return getRestHelper().createPullRequestComment(owner, repo, env.pullRequest().id(), body);
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
    @SpelFunction(cat=ci, desc="Adds a review comment on a specific file and line in the pull request detected from the workflow run",
            returns="Created review comment object")
    public ObjectNode addReviewComment(
            @SpelFunctionParam(name="path", desc="file path relative to repository root") String path,
            @SpelFunctionParam(name="line", desc="line number") int line,
            @SpelFunctionParam(name="body", desc="comment body (Markdown supported)") String body) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in pull request context. GITHUB_HEAD_REF is not set.");
        }
        var repoName = env.ciRepository().name();
        var owner = repoName.full().contains("/") ? repoName.full().split("/")[0] : "";
        var repo = repoName.short_();
        var sha = env.ciCommit().id().full();
        return getRestHelper().createReviewComment(owner, repo, env.pullRequest().id(), sha, path, line, body);
    }
    
    // === REST Helper Access ===
    
    private GitHubRestHelper getRestHelper() {
        if (restHelper == null) {
            var supplier = GitHubUnirestInstanceSupplier.fromEnv(ctx.getUnirestContext());
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
