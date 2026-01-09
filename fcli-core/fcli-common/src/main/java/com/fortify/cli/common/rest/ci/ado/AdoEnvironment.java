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
package com.fortify.cli.common.rest.ci.ado;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.EnvHelper;

import lombok.Builder;

/**
 * Immutable record holding detected Azure DevOps environment data.
 * Provides context-aware branch detection for both regular commits and pull requests.
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record AdoEnvironment(
    String organization,
    String project,
    String repositoryName,
    String sourceBranch,
    String targetBranch,
    String sha,
    String sourceDir,
    boolean isPullRequest,
    Integer pullRequestId
) {
    // Environment variable names
    public static final String ENV_ORGANIZATION_URL = "System.TeamFoundationCollectionUri";
    public static final String ENV_PROJECT = "System.TeamProject";
    public static final String ENV_REPOSITORY_NAME = "Build.Repository.Name";
    public static final String ENV_SOURCE_BRANCH = "Build.SourceBranch";
    public static final String ENV_SOURCE_BRANCH_NAME = "Build.SourceBranchName";
    public static final String ENV_SOURCE_VERSION = "Build.SourceVersion";
    public static final String ENV_SOURCES_DIRECTORY = "Build.SourcesDirectory";
    public static final String ENV_DEFAULT_WORKING_DIRECTORY = "System.DefaultWorkingDirectory";
    public static final String ENV_SOURCE_DIR = "SOURCE_DIR";
    public static final String ENV_PR_SOURCE_BRANCH = "System.PullRequest.SourceBranch";
    public static final String ENV_PR_SOURCE_BRANCH_NAME = "System.PullRequest.SourceBranchName";
    public static final String ENV_PR_TARGET_BRANCH = "System.PullRequest.TargetBranch";
    public static final String ENV_PR_TARGET_BRANCH_NAME = "System.PullRequest.TargetBranchName";
    public static final String ENV_PR_ID = "System.PullRequest.PullRequestId";
    public static final String ENV_TOKEN = "AZURE_DEVOPS_TOKEN";
    
    /**
     * Detect Azure DevOps CI environment from environment variables.
     * Returns null if not running in Azure DevOps.
     */
    public static AdoEnvironment detect() {
        var repoName = EnvHelper.env(ENV_REPOSITORY_NAME);
        if (repoName == null) return null;
        
        var sourceBranchRaw = EnvHelper.env(ENV_SOURCE_BRANCH);
        var isPr = sourceBranchRaw != null && sourceBranchRaw.startsWith("refs/pull/");
        var branchInfo = detectBranchInfo(isPr, sourceBranchRaw);
        
        return AdoEnvironment.builder()
            .organization(EnvHelper.env(ENV_ORGANIZATION_URL))
            .project(EnvHelper.env(ENV_PROJECT))
            .repositoryName(repoName)
            .sourceBranch(branchInfo[0])
            .targetBranch(branchInfo[1])
            .sha(EnvHelper.env(ENV_SOURCE_VERSION))
            .sourceDir(EnvHelper.envOrDefault(ENV_SOURCE_DIR,
                EnvHelper.envOrDefault(ENV_SOURCES_DIRECTORY,
                    EnvHelper.envOrDefault(ENV_DEFAULT_WORKING_DIRECTORY, "."))))
            .isPullRequest(isPr)
            .pullRequestId(parseIntOrNull(EnvHelper.env(ENV_PR_ID)))
            .build();
    }
    
    /**
     * Detect branch information based on context.
     * Returns [sourceBranch, targetBranch]
     */
    private static String[] detectBranchInfo(boolean isPr, String sourceBranchRaw) {
        String sourceBranch;
        String targetBranch;
        
        if (isPr) {
            sourceBranch = EnvHelper.envOrDefault(ENV_PR_SOURCE_BRANCH,
                EnvHelper.env(ENV_PR_SOURCE_BRANCH_NAME));
            sourceBranch = sourceBranch != null ? sourceBranch.replaceAll("^refs/heads/", "") : null;
            
            targetBranch = EnvHelper.envOrDefault(ENV_PR_TARGET_BRANCH,
                EnvHelper.env(ENV_PR_TARGET_BRANCH_NAME));
            targetBranch = targetBranch != null ? targetBranch.replaceAll("^refs/heads/", "") : null;
        } else {
            sourceBranch = EnvHelper.envOrDefault(ENV_SOURCE_BRANCH_NAME,
                sourceBranchRaw != null ? sourceBranchRaw.replaceAll("^refs/heads/", "") : null);
            targetBranch = null;
        }
        
        return new String[]{sourceBranch, targetBranch};
    }
    
    /**
     * Get qualified repository name for Fortify (repo:branch format).
     * Uses source branch for PRs, current branch for regular commits.
     */
    public String getQualifiedRepoName() {
        return sourceBranch != null
            ? repositoryName + ":" + sourceBranch
            : repositoryName;
    }
    
    /**
     * Get branch name suitable for FoD/SSC application version naming.
     * Returns source branch for PRs, current branch otherwise.
     */
    public String getBranchForVersioning() {
        return sourceBranch;
    }
    
    private static Integer parseIntOrNull(String value) {
        return value != null ? Integer.parseInt(value) : null;
    }
}
