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
package com.fortify.cli.common.action.helper.ci.github;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.ci;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.ci.github.GitHubEnvironment;
import com.fortify.cli.common.ci.github.GitHubRepo;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctions;

import lombok.RequiredArgsConstructor;

/**
 * Action wrapper for GitHubRepo providing environment-aware GitHub repository operations.
 * Used by ActionGitHubSpelFunctions to provide SpEL functions that automatically 
 * use GitHub Actions environment defaults.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
@SpelFunctions
public class ActionGitHubRepo {
    private final GitHubRepo repo;
    private final GitHubEnvironment env;
    
    @SpelFunction(cat=ci, desc="Uploads SARIF to GitHub Code Scanning (paid tier, requires GHAS) using repository/ref/commit from the current workflow run. Throws GhasUnavailableException if GitHub Advanced Security is not enabled. Use on.fail in action YAML to implement fallback logic.",
            returns="Response from GitHub API")
    public ObjectNode uploadSarif(
            @SpelFunctionParam(name="sarifContent", desc="SARIF report content as string") String sarifContent) {
        var ref = env.ciBranch().full();
        var sha = env.ciCommit().mergeId().full();  // GitHub API requires merge commit for PRs
        return repo.uploadSarif(ref, sarifContent, sha);
    }
    
    @SpelFunction(cat=ci, desc="Creates check run with optional annotations (free tier, auto-paginates >50 annotations). Accepts body object per GitHub REST API spec (https://docs.github.com/en/rest/checks/runs). Automatically adds head_sha from detected commit if not present.",
            returns="Response from GitHub API")
    public ObjectNode createCheckRun(
            @SpelFunctionParam(name="body", desc="check run body object: {name, head_sha (optional), status, conclusion (optional), started_at (optional), completed_at (optional), output: {title, summary, annotations (optional)}}") ObjectNode body) {
        if (!body.has("head_sha")) {
            body.put("head_sha", env.ciCommit().headId().full());  // Head commit better for annotations on actual code
        }
        return repo.createCheckRun(body);
    }
    
    @SpelFunction(cat=ci, desc="(PREVIEW) Adds a comment to the current pull request detected from the workflow run. This function is not yet used by any built-in fcli actions; signature and implementation may change in future fcli versions based on new insights as to how to best integrate this functionality into fcli built-in actions.",
            returns="Created comment object")
    public ObjectNode addPrComment(
            @SpelFunctionParam(name="body", desc="comment body (Markdown supported)") String body) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in pull request context. GITHUB_HEAD_REF is not set.");
        }
        return repo.createPullRequestComment(env.pullRequest().id(), body);
    }
    
    @SpelFunction(cat=ci, desc="(PREVIEW) Adds a review comment on a specific file and line in the pull request detected from the workflow run. This function is not yet used by any built-in fcli actions; signature and implementation may change in future fcli versions based on new insights as to how to best integrate this functionality into fcli built-in actions.",
            returns="Created review comment object")
    public ObjectNode addReviewComment(
            @SpelFunctionParam(name="path", desc="file path relative to repository root") String path,
            @SpelFunctionParam(name="line", desc="line number") int line,
            @SpelFunctionParam(name="body", desc="comment body (Markdown supported)") String body) {
        if (!env.pullRequest().active()) {
            throw new FcliSimpleException("Not running in pull request context. GITHUB_HEAD_REF is not set.");
        }
        var sha = env.ciCommit().headId().full();  // Use head commit for review comments on actual PR code
        return repo.createReviewComment(env.pullRequest().id(), sha, path, line, body);
    }
}
