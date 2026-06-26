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
    String token,
    String buildId,
    String prTerminology,
    String prKeyword,
    String ciName,
    String ciId
) {
    // CI system type identifier
    public static final String TYPE = "ado";
    public static final String NAME = "Azure DevOps";
    public static final String ID = "ado";
    public static final String PR_TERMINOLOGY = "Pull Request";
    public static final String PR_KEYWORD = "pr";
    
    // Environment variable names (arrays for fallback lookup)
    public static final String[] ENV_ORGANIZATION_URL = {"System.TeamFoundationCollectionUri", "SYSTEM_TEAMFOUNDATIONCOLLECTIONURI"};
    public static final String[] ENV_PROJECT = {"System.TeamProject", "SYSTEM_TEAMPROJECT"};
    public static final String[] ENV_REPOSITORY_NAME = {"Build.Repository.Name", "BUILD_REPOSITORY_NAME"};
    public static final String[] ENV_REPOSITORY_ID = {"Build.Repository.ID", "BUILD_REPOSITORY_ID"};
    public static final String[] ENV_BUILD_ID = {"Build.BuildId", "BUILD_BUILDID"};
    public static final String[] ENV_SOURCE_BRANCH = {"Build.SourceBranch", "BUILD_SOURCEBRANCH"};
    public static final String[] ENV_SOURCE_BRANCH_NAME = {"Build.SourceBranchName", "BUILD_SOURCEBRANCHNAME"};
    public static final String[] ENV_SOURCE_VERSION = {"Build.SourceVersion", "BUILD_SOURCEVERSION"};
    public static final String[] ENV_SOURCES_DIRECTORY = {"Build.SourcesDirectory", "BUILD_SOURCESDIRECTORY"};
    public static final String[] ENV_DEFAULT_WORKING_DIRECTORY = {"System.DefaultWorkingDirectory", "SYSTEM_DEFAULTWORKINGDIRECTORY"};
    public static final String[] ENV_PR_SOURCE_BRANCH = {"System.PullRequest.SourceBranch", "SYSTEM_PULLREQUEST_SOURCEBRANCH"};
    public static final String[] ENV_PR_SOURCE_BRANCH_NAME = {"System.PullRequest.SourceBranchName", "SYSTEM_PULLREQUEST_SOURCEBRANCHNAME"};
    public static final String[] ENV_PR_TARGET_BRANCH = {"System.PullRequest.TargetBranch", "SYSTEM_PULLREQUEST_TARGETBRANCH"};
    public static final String[] ENV_PR_TARGET_BRANCH_NAME = {"System.PullRequest.TargetBranchName", "SYSTEM_PULLREQUEST_TARGETBRANCHNAME"};
    public static final String[] ENV_PR_ID = {"System.PullRequest.PullRequestId", "SYSTEM_PULLREQUEST_PULLREQUESTID"};
    public static final String[] ENV_TOKEN = {"ADO_TOKEN", "SYSTEM_ACCESSTOKEN"};
    
    /**
     * Detect Azure DevOps CI environment from environment variables.
     * Returns null if not running in Azure DevOps.
     */
    public static AdoEnvironment detect() {
        var orgUrl = env(ENV_ORGANIZATION_URL);
        if (StringUtils.isBlank(orgUrl)) return null;

        var repoName = env(ENV_REPOSITORY_NAME);
        var sourceBranchRaw = env(ENV_SOURCE_BRANCH);
        var isPr = StringUtils.isNotBlank(sourceBranchRaw) && sourceBranchRaw.startsWith("refs/pull/");
        var branchInfo = detectBranchInfo(isPr, sourceBranchRaw);
        var sourceBranch = branchInfo[0];
        var targetBranch = branchInfo[1];
        var sha = env(ENV_SOURCE_VERSION);
        var repositoryId = env(ENV_REPOSITORY_ID);
        var buildId = env(ENV_BUILD_ID);
        
        // Build standardized structures
        // Extract simple repo name from full path if present
        String shortRepoName = repoName;
        String fullRepoName = repoName;
        if (repoName.contains("/")) {
            var parts = repoName.split("/");
            shortRepoName = parts[parts.length - 1];
        }
        
        var ciRepository = CiRepository.builder()
            .workspaceDir(StringUtils.defaultIfBlank(env(ENV_SOURCES_DIRECTORY, ENV_DEFAULT_WORKING_DIRECTORY), "."))
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
            ? CiPullRequest.active(env(ENV_PR_ID), targetBranch)
            : CiPullRequest.inactive();
        
        return AdoEnvironment.builder()
            .organization(orgUrl)
            .project(env(ENV_PROJECT))
            .repositoryId(repositoryId)
            .token(env(ENV_TOKEN))
            .buildId(buildId)
            .ciRepository(ciRepository)
            .ciBranch(ciBranch)
            .ciCommit(ciCommit)
            .pullRequest(pullRequest)
            .prTerminology(PR_TERMINOLOGY)
            .prKeyword(PR_KEYWORD)
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
            sourceBranch = env(ENV_PR_SOURCE_BRANCH, ENV_PR_SOURCE_BRANCH_NAME);
            sourceBranch = StringUtils.isNotBlank(sourceBranch) ? sourceBranch.replaceAll("^refs/heads/", "") : null;
            
            targetBranch = env(ENV_PR_TARGET_BRANCH, ENV_PR_TARGET_BRANCH_NAME);
            targetBranch = StringUtils.isNotBlank(targetBranch) ? targetBranch.replaceAll("^refs/heads/", "") : null;
        } else {
            sourceBranch = env(ENV_SOURCE_BRANCH_NAME);
            if (StringUtils.isBlank(sourceBranch) && StringUtils.isNotBlank(sourceBranchRaw)) {
                sourceBranch = sourceBranchRaw.replaceAll("^refs/heads/", "");
            }
            targetBranch = null;
        }
        
        return new String[]{sourceBranch, targetBranch};
    }

    private static String env(String[]... envNames) {
        return EnvHelper.env(envNames);
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
