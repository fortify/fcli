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

import org.apache.commons.lang3.StringUtils;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.ci.CiBranch;
import com.fortify.cli.common.ci.CiCommit;
import com.fortify.cli.common.ci.CiCommitId;
import com.fortify.cli.common.ci.CiPullRequest;
import com.fortify.cli.common.ci.CiRepository;
import com.fortify.cli.common.ci.CiRepositoryName;
import com.fortify.cli.common.util.EnvHelper;

import lombok.Builder;

/**
 * Immutable record holding detected GitLab CI environment data.
 * Provides context-aware branch detection for both regular commits and merge requests.
 * 
 * <p>This class provides standardized nested structures ({@link CiRepository}, {@link CiBranch},
 * {@link CiCommit}) that match the format returned by the {@code localRepo()} SpEL function,
 * plus GitLab-specific properties.
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record GitLabEnvironment(
    // Standardized nested structures matching localRepo() format
    CiRepository ciRepository,
    CiBranch ciBranch,
    CiCommit ciCommit,
    CiPullRequest pullRequest,
    // GitLab-specific properties
    String projectId,
    String pipelineId,
    String prTerminology,
    String ciName,
    String ciId
) {
    // CI system type identifier
    public static final String TYPE = "gitlab";
    public static final String NAME = "GitLab";
    public static final String ID = "gitlab";
    public static final String PR_TERMINOLOGY = "Merge Request";
    
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
    public static final String ENV_SERVER_URL = "CI_SERVER_URL"; // Base GitLab URL
    public static final String ENV_API_V4_URL = "CI_API_V4_URL"; // API v4 URL
    public static final String ENV_TOKEN = "GITLAB_TOKEN";
    public static final String ENV_JOB_TOKEN = "CI_JOB_TOKEN"; // Built-in job token (automatic)
    
    /**
     * Detect GitLab CI environment from environment variables.
     * Returns null if not running in GitLab CI.
     */
    public static GitLabEnvironment detect() {
        if (!"true".equals(EnvHelper.env(ENV_GITLAB_CI))) return null;
        
        var projectIdStr = EnvHelper.env(ENV_PROJECT_ID);
        var isMr = StringUtils.isNotBlank(EnvHelper.env(ENV_MR_IID));
        var branchInfo = detectBranchInfo(isMr);
        var projectPath = detectProjectPath();
        var projectName = EnvHelper.env(ENV_PROJECT_NAME);
        var sha = EnvHelper.env(ENV_COMMIT_SHA);
        var sourceBranch = branchInfo[0];
        var targetBranch = branchInfo[1];
        
        // Build standardized structures
        var ciRepository = CiRepository.builder()
            .workspaceDir(EnvHelper.envOrDefault(ENV_PROJECT_DIR, "."))
            .remoteUrl(EnvHelper.env(ENV_REPOSITORY_URL))
            .name(CiRepositoryName.builder()
                .short_(projectName)
                .full(projectPath)
                .build())
            .build();
        
        // Construct full ref for consistency with GitHub/localRepo
        String fullRef = null;
        if (isMr) {
            fullRef = String.format("refs/merge-requests/%s/head", EnvHelper.env(ENV_MR_IID));
        } else if (StringUtils.isNotBlank(sourceBranch)) {
            fullRef = "refs/heads/" + sourceBranch;
        }
        
        var ciBranch = CiBranch.builder()
            .full(fullRef)
            .short_(sourceBranch)
            .build();
        
        var ciCommit = CiCommit.builder()
            .id(CiCommitId.builder()
                .full(sha)
                .short_(StringUtils.isNotBlank(sha) && sha.length() >= 7 ? sha.substring(0, 7) : sha)
                .build())
            .message(null)  // Not available in GitLab CI environment
            .author(null)   // Not available in GitLab CI environment
            .committer(null)  // Not available in GitLab CI environment
            .build();
        
        var pullRequest = isMr
            ? CiPullRequest.active(EnvHelper.env(ENV_MR_IID), targetBranch)
            : CiPullRequest.inactive();
        
        var pipelineIdValue = EnvHelper.env(ENV_PIPELINE_ID);
        
        return GitLabEnvironment.builder()
            .projectId(projectIdStr)
            .pipelineId(StringUtils.isNotBlank(pipelineIdValue) ? pipelineIdValue : null)
            .ciRepository(ciRepository)
            .ciBranch(ciBranch)
            .ciCommit(ciCommit)
            .pullRequest(pullRequest)
            .prTerminology(PR_TERMINOLOGY)
            .ciName(NAME)
            .ciId(ID)
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
        return StringUtils.isNotBlank(repoUrl)
            ? repoUrl.replaceAll("[^:]+://[^/]+/", "").replaceAll("\\.git$", "")
            : EnvHelper.env(ENV_PROJECT_PATH);
    }
    
    /**
     * Get qualified repository name for Fortify (project/path:branch format).
     * Uses source branch for MRs, current branch for regular commits.
     */
    public String getQualifiedRepoName() {
        var branch = ciBranch != null ? ciBranch.short_() : null;
        var repoFull = ciRepository != null && ciRepository.name() != null ? ciRepository.name().full() : null;
        return branch != null && repoFull != null
            ? repoFull + ":" + branch
            : repoFull;
    }
    
    /**
     * Get branch name suitable for FoD/SSC application version naming.
     * Returns source branch for MRs, current branch otherwise.
     */
    public String getBranchForVersioning() {
        return ciBranch != null ? ciBranch.short_() : null;
    }
}
