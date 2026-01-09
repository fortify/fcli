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
package com.fortify.cli.common.rest.ci.gitlab;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.util.EnvHelper;

import lombok.Builder;

/**
 * Immutable record holding detected GitLab CI environment data.
 * Provides context-aware branch detection for both regular commits and merge requests.
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record GitLabEnvironment(
    int projectId,
    String projectName,
    String projectPath,
    String sourceBranch,
    String targetBranch,
    String sha,
    String sourceDir,
    boolean isMergeRequest,
    Integer mergeRequestId,
    Integer pipelineId
) {
    // Environment variable names
    public static final String ENV_GITLAB_CI = "GITLAB_CI";
    public static final String ENV_PROJECT_ID = "CI_PROJECT_ID";
    public static final String ENV_PROJECT_NAME = "CI_PROJECT_NAME";
    public static final String ENV_PROJECT_PATH = "CI_PROJECT_PATH";
    public static final String ENV_PROJECT_DIR = "CI_PROJECT_DIR";
    public static final String ENV_COMMIT_SHA = "CI_COMMIT_SHA";
    public static final String ENV_COMMIT_BRANCH = "CI_COMMIT_BRANCH";
    public static final String ENV_MR_IID = "CI_MERGE_REQUEST_IID";
    public static final String ENV_MR_SOURCE_BRANCH = "CI_MERGE_REQUEST_SOURCE_BRANCH_NAME";
    public static final String ENV_MR_TARGET_BRANCH = "CI_MERGE_REQUEST_TARGET_BRANCH_NAME";
    public static final String ENV_PIPELINE_ID = "CI_PIPELINE_ID";
    public static final String ENV_REPOSITORY_URL = "CI_REPOSITORY_URL";
    public static final String ENV_SOURCE_DIR = "SOURCE_DIR";
    public static final String ENV_SERVER_URL = "CI_SERVER_URL"; // Base GitLab URL
    public static final String ENV_API_V4_URL = "CI_API_V4_URL"; // API v4 URL
    public static final String ENV_TOKEN = "GITLAB_TOKEN";
    
    /**
     * Detect GitLab CI environment from environment variables.
     * Returns null if not running in GitLab CI.
     */
    public static GitLabEnvironment detect() {
        if (!"true".equals(EnvHelper.env(ENV_GITLAB_CI))) return null;
        
        var projectIdStr = EnvHelper.env(ENV_PROJECT_ID);
        var isMr = EnvHelper.env(ENV_MR_IID) != null;
        var branchInfo = detectBranchInfo(isMr);
        var projectPath = detectProjectPath();
        
        return GitLabEnvironment.builder()
            .projectId(projectIdStr != null ? Integer.parseInt(projectIdStr) : 0)
            .projectName(EnvHelper.env(ENV_PROJECT_NAME))
            .projectPath(projectPath)
            .sourceBranch(branchInfo[0])
            .targetBranch(branchInfo[1])
            .sha(EnvHelper.env(ENV_COMMIT_SHA))
            .sourceDir(EnvHelper.envOrDefault(ENV_SOURCE_DIR,
                EnvHelper.envOrDefault(ENV_PROJECT_DIR, ".")))
            .isMergeRequest(isMr)
            .mergeRequestId(parseIntOrNull(EnvHelper.env(ENV_MR_IID)))
            .pipelineId(parseIntOrNull(EnvHelper.env(ENV_PIPELINE_ID)))
            .build();
    }
    
    /**
     * Detect branch information based on context.
     * Returns [sourceBranch, targetBranch]
     */
    private static String[] detectBranchInfo(boolean isMr) {
        String sourceBranch = isMr
            ? EnvHelper.env(ENV_MR_SOURCE_BRANCH)
            : EnvHelper.env(ENV_COMMIT_BRANCH);
        
        String targetBranch = isMr
            ? EnvHelper.env(ENV_MR_TARGET_BRANCH)
            : null;
        
        return new String[]{sourceBranch, targetBranch};
    }
    
    /**
     * Extract project path from repository URL or fallback to project path variable.
     */
    private static String detectProjectPath() {
        var repoUrl = EnvHelper.env(ENV_REPOSITORY_URL);
        return repoUrl != null
            ? repoUrl.replaceAll("[^:]+://[^/]+/", "").replaceAll("\\.git$", "")
            : EnvHelper.env(ENV_PROJECT_PATH);
    }
    
    /**
     * Get qualified repository name for Fortify (project/path:branch format).
     * Uses source branch for MRs, current branch for regular commits.
     */
    public String getQualifiedRepoName() {
        return sourceBranch != null
            ? projectPath + ":" + sourceBranch
            : projectPath;
    }
    
    /**
     * Get branch name suitable for FoD/SSC application version naming.
     * Returns source branch for MRs, current branch otherwise.
     */
    public String getBranchForVersioning() {
        return sourceBranch;
    }
    
    private static Integer parseIntOrNull(String value) {
        return value != null ? Integer.parseInt(value) : null;
    }
}
