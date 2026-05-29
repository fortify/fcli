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
package com.fortify.cli.common.ci.ado;

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
 * Immutable record holding detected Azure DevOps environment data.
 * Provides context-aware branch detection for both regular commits and pull requests.
 * 
 * <p>This class provides standardized nested structures ({@link CiRepository}, {@link CiBranch},
 * {@link CiCommit}) that match the format returned by the {@code localRepo()} SpEL function,
 * plus Azure DevOps-specific properties.
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record AdoEnvironment(
    // Standardized nested structures matching localRepo() format
    CiRepository ciRepository,
    CiBranch ciBranch,
    CiCommit ciCommit,
    CiPullRequest pullRequest,
    // Azure DevOps-specific properties
    String organization,
    String project,
    String repositoryId,
    String buildId,
    String prTerminology,
    String ciName,
    String ciId
) {
    // CI system type identifier
    public static final String TYPE = "ado";
    public static final String NAME = "Azure DevOps";
    public static final String ID = "ado";
    public static final String PR_TERMINOLOGY = "Pull Request";
    
    // Environment variable names
    public static final String ENV_ORGANIZATION_URL = "System.TeamFoundationCollectionUri";
    public static final String ENV_PROJECT = "System.TeamProject";
    public static final String ENV_REPOSITORY_NAME = "Build.Repository.Name";
    public static final String ENV_REPOSITORY_ID = "Build.Repository.ID";
    public static final String ENV_BUILD_ID = "Build.BuildId";
    public static final String ENV_SOURCE_BRANCH = "Build.SourceBranch";
    public static final String ENV_SOURCE_BRANCH_NAME = "Build.SourceBranchName";
    public static final String ENV_SOURCE_VERSION = "Build.SourceVersion";
    public static final String ENV_SOURCES_DIRECTORY = "Build.SourcesDirectory";
    public static final String ENV_DEFAULT_WORKING_DIRECTORY = "System.DefaultWorkingDirectory";
    public static final String ENV_PR_SOURCE_BRANCH = "System.PullRequest.SourceBranch";
    public static final String ENV_PR_SOURCE_BRANCH_NAME = "System.PullRequest.SourceBranchName";
    public static final String ENV_PR_TARGET_BRANCH = "System.PullRequest.TargetBranch";
    public static final String ENV_PR_TARGET_BRANCH_NAME = "System.PullRequest.TargetBranchName";
    public static final String ENV_PR_ID = "System.PullRequest.PullRequestId";
    public static final String ENV_TOKEN = "ADO_TOKEN";
    
    /**
     * Detect Azure DevOps CI environment from environment variables.
     * Returns null if not running in Azure DevOps.
     */
    public static AdoEnvironment detect() {
        var repoName = EnvHelper.env(ENV_REPOSITORY_NAME);
        if (StringUtils.isBlank(repoName)) return null;
        
        var sourceBranchRaw = EnvHelper.env(ENV_SOURCE_BRANCH);
        var isPr = StringUtils.isNotBlank(sourceBranchRaw) && sourceBranchRaw.startsWith("refs/pull/");
        var branchInfo = detectBranchInfo(isPr, sourceBranchRaw);
        var sourceBranch = branchInfo[0];
        var targetBranch = branchInfo[1];
        var sha = EnvHelper.env(ENV_SOURCE_VERSION);
        var repositoryId = EnvHelper.env(ENV_REPOSITORY_ID);
        var buildId = EnvHelper.env(ENV_BUILD_ID);
        
        // Build standardized structures
        // Extract simple repo name from full path if present
        String shortRepoName = repoName;
        String fullRepoName = repoName;
        if (repoName.contains("/")) {
            var parts = repoName.split("/");
            shortRepoName = parts[parts.length - 1];
        }
        
        var ciRepository = CiRepository.builder()
            .workspaceDir(EnvHelper.envOrDefault(ENV_SOURCES_DIRECTORY,
                EnvHelper.envOrDefault(ENV_DEFAULT_WORKING_DIRECTORY, ".")))
            .remoteUrl(null)  // Not readily available in environment
            .name(CiRepositoryName.builder()
                .short_(shortRepoName)
                .full(fullRepoName)
                .build())
            .build();
        
        var ciBranch = CiBranch.builder()
            .full(sourceBranchRaw)
            .short_(sourceBranch)
            .build();
        
        var ciCommit = CiCommit.builder()
            .headId(CiCommitId.builder()
                .full(sha)
                .short_(StringUtils.isNotBlank(sha) && sha.length() >= 7 ? sha.substring(0, 7) : sha)
                .build())
            .mergeId(CiCommitId.builder()
                .full(sha)
                .short_(StringUtils.isNotBlank(sha) && sha.length() >= 7 ? sha.substring(0, 7) : sha)
                .build())
            .message(null)  // Not available in Azure DevOps environment
            .author(null)   // Not available in Azure DevOps environment
            .committer(null)  // Not available in Azure DevOps environment
            .build();
        
        var pullRequest = isPr
            ? CiPullRequest.active(EnvHelper.env(ENV_PR_ID), targetBranch)
            : CiPullRequest.inactive();
        
        return AdoEnvironment.builder()
            .organization(EnvHelper.env(ENV_ORGANIZATION_URL))
            .project(EnvHelper.env(ENV_PROJECT))
            .repositoryId(repositoryId)
            .buildId(buildId)
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
    private static String[] detectBranchInfo(boolean isPr, String sourceBranchRaw) {
        String sourceBranch;
        String targetBranch;
        
        if (isPr) {
            sourceBranch = EnvHelper.envOrDefault(ENV_PR_SOURCE_BRANCH,
                EnvHelper.env(ENV_PR_SOURCE_BRANCH_NAME));
            sourceBranch = StringUtils.isNotBlank(sourceBranch) ? sourceBranch.replaceAll("^refs/heads/", "") : null;
            
            targetBranch = EnvHelper.envOrDefault(ENV_PR_TARGET_BRANCH,
                EnvHelper.env(ENV_PR_TARGET_BRANCH_NAME));
            targetBranch = StringUtils.isNotBlank(targetBranch) ? targetBranch.replaceAll("^refs/heads/", "") : null;
        } else {
            sourceBranch = EnvHelper.envOrDefault(ENV_SOURCE_BRANCH_NAME,
                StringUtils.isNotBlank(sourceBranchRaw) ? sourceBranchRaw.replaceAll("^refs/heads/", "") : null);
            targetBranch = null;
        }
        
        return new String[]{sourceBranch, targetBranch};
    }
    
    /**
     * Get qualified repository name for Fortify (repo:branch format).
     * Uses source branch for PRs, current branch for regular commits.
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
     * Returns source branch for PRs, current branch otherwise.
     */
    public String getBranchForVersioning() {
        return ciBranch != null ? ciBranch.short_() : null;
    }
}
