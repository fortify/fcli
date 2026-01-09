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

import java.util.regex.Pattern;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.EnvHelper;

import lombok.Builder;

/**
 * Immutable record holding detected GitHub Actions environment data.
 * Provides context-aware branch detection for both regular commits and pull requests.
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record GitHubEnvironment(
    String owner,
    String repository,
    String repositoryFullName,
    String sourceBranch,
    String targetBranch,
    String ref,
    String sha,
    String workspaceDir,
    String jobSummaryFile,
    boolean isPullRequest,
    Integer pullRequestNumber
) {
    private static final Pattern PR_NUMBER_PATTERN = Pattern.compile("refs/pull/(\\d+)/");
    
    // Environment variable names
    public static final String ENV_REPOSITORY = "GITHUB_REPOSITORY";
    public static final String ENV_REF = "GITHUB_REF";
    public static final String ENV_REF_NAME = "GITHUB_REF_NAME";
    public static final String ENV_SHA = "GITHUB_SHA";
    public static final String ENV_HEAD_REF = "GITHUB_HEAD_REF";
    public static final String ENV_BASE_REF = "GITHUB_BASE_REF";
    public static final String ENV_WORKSPACE = "GITHUB_WORKSPACE";
    public static final String ENV_STEP_SUMMARY = "GITHUB_STEP_SUMMARY";
    public static final String ENV_SOURCE_DIR = "SOURCE_DIR";
    public static final String ENV_SERVER_URL = "GITHUB_SERVER_URL"; // Base URL for GitHub Enterprise
    public static final String ENV_API_URL = "GITHUB_API_URL"; // API URL for GitHub Enterprise
    public static final String ENV_TOKEN = "GITHUB_TOKEN";
    
    /**
     * Detect GitHub Actions environment from environment variables.
     * Returns null if not running in GitHub Actions.
     */
    public static GitHubEnvironment detect() {
        var ghRepo = EnvHelper.env(ENV_REPOSITORY);
        if (ghRepo == null) return null;
        
        var ref = EnvHelper.env(ENV_REF);
        var isPr = isPullRequest(ref);
        var branchInfo = detectBranchInfo(ref, isPr);
        var prInfo = isPr ? detectPullRequestInfo(ref) : null;
        var repoParts = ghRepo.split("/", 2);
        
        return GitHubEnvironment.builder()
            .owner(repoParts[0])
            .repository(repoParts.length > 1 ? repoParts[1] : ghRepo)
            .repositoryFullName(ghRepo)
            .sourceBranch(branchInfo[0])
            .targetBranch(branchInfo[1])
            .ref(ref)
            .sha(EnvHelper.env(ENV_SHA))
            .workspaceDir(EnvHelper.envOrDefault(ENV_SOURCE_DIR, 
                EnvHelper.envOrDefault(ENV_WORKSPACE, ".")))
            .jobSummaryFile(EnvHelper.env(ENV_STEP_SUMMARY))
            .isPullRequest(isPr)
            .pullRequestNumber(prInfo)
            .build();
    }
    
    private static boolean isPullRequest(String ref) {
        return ref != null && ref.startsWith("refs/pull/");
    }
    
    /**
     * Detect branch information based on context.
     * Returns [sourceBranch, targetBranch]
     */
    private static String[] detectBranchInfo(String ref, boolean isPr) {
        String sourceBranch;
        String targetBranch;
        
        if (isPr) {
            sourceBranch = EnvHelper.env(ENV_HEAD_REF);
            targetBranch = EnvHelper.env(ENV_BASE_REF);
        } else {
            sourceBranch = EnvHelper.env(ENV_REF_NAME);
            targetBranch = null;
        }
        
        return new String[]{sourceBranch, targetBranch};
    }
    
    /**
     * Extract pull request number from ref.
     */
    private static Integer detectPullRequestInfo(String ref) {
        if (ref == null) return null;
        var matcher = PR_NUMBER_PATTERN.matcher(ref);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }
    
    /**
     * Get qualified repository name for Fortify (owner/repo:branch format).
     * Uses source branch for PRs, current branch for regular commits.
     */
    public String getQualifiedRepoName() {
        return sourceBranch != null 
            ? repositoryFullName + ":" + sourceBranch
            : repositoryFullName;
    }
    
    /**
     * Get branch name suitable for FoD/SSC application version naming.
     * Returns source branch for PRs, current branch otherwise.
     */
    public String getBranchForVersioning() {
        return sourceBranch;
    }
}
