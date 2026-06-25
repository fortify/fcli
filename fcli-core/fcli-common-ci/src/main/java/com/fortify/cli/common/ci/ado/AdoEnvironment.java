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
    String prCommentActionSuffix,
    String ciName,
    String ciId
) {
    // CI system type identifier
    public static final String TYPE = "ado";
    public static final String NAME = "Azure DevOps";
    public static final String ID = "ado";
    public static final String PR_TERMINOLOGY = "Pull Request";
    public static final String PR_COMMENT_ACTION_SUFFIX = "pr-comment";
    
    // Environment variable names
    public static final String ENV_ORGANIZATION_URL = "System.TeamFoundationCollectionUri";
    public static final String ENV_ORGANIZATION_URL_ALT = "SYSTEM_TEAMFOUNDATIONCOLLECTIONURI";
    public static final String ENV_PROJECT = "System.TeamProject";
    public static final String ENV_PROJECT_ALT = "SYSTEM_TEAMPROJECT";
    public static final String ENV_REPOSITORY_NAME = "Build.Repository.Name";
    public static final String ENV_REPOSITORY_NAME_ALT = "BUILD_REPOSITORY_NAME";
    public static final String ENV_REPOSITORY_ID = "Build.Repository.ID";
    public static final String ENV_REPOSITORY_ID_ALT = "BUILD_REPOSITORY_ID";
    public static final String ENV_BUILD_ID = "Build.BuildId";
    public static final String ENV_BUILD_ID_ALT = "BUILD_BUILDID";
    public static final String ENV_SOURCE_BRANCH = "Build.SourceBranch";
    public static final String ENV_SOURCE_BRANCH_ALT = "BUILD_SOURCEBRANCH";
    public static final String ENV_SOURCE_BRANCH_NAME = "Build.SourceBranchName";
    public static final String ENV_SOURCE_BRANCH_NAME_ALT = "BUILD_SOURCEBRANCHNAME";
    public static final String ENV_SOURCE_VERSION = "Build.SourceVersion";
    public static final String ENV_SOURCE_VERSION_ALT = "BUILD_SOURCEVERSION";
    public static final String ENV_SOURCES_DIRECTORY = "Build.SourcesDirectory";
    public static final String ENV_SOURCES_DIRECTORY_ALT = "BUILD_SOURCESDIRECTORY";
    public static final String ENV_DEFAULT_WORKING_DIRECTORY = "System.DefaultWorkingDirectory";
    public static final String ENV_DEFAULT_WORKING_DIRECTORY_ALT = "SYSTEM_DEFAULTWORKINGDIRECTORY";
    public static final String ENV_PR_SOURCE_BRANCH = "System.PullRequest.SourceBranch";
    public static final String ENV_PR_SOURCE_BRANCH_ALT = "SYSTEM_PULLREQUEST_SOURCEBRANCH";
    public static final String ENV_PR_SOURCE_BRANCH_NAME = "System.PullRequest.SourceBranchName";
    public static final String ENV_PR_SOURCE_BRANCH_NAME_ALT = "SYSTEM_PULLREQUEST_SOURCEBRANCHNAME";
    public static final String ENV_PR_TARGET_BRANCH = "System.PullRequest.TargetBranch";
    public static final String ENV_PR_TARGET_BRANCH_ALT = "SYSTEM_PULLREQUEST_TARGETBRANCH";
    public static final String ENV_PR_TARGET_BRANCH_NAME = "System.PullRequest.TargetBranchName";
    public static final String ENV_PR_TARGET_BRANCH_NAME_ALT = "SYSTEM_PULLREQUEST_TARGETBRANCHNAME";
    public static final String ENV_PR_ID = "System.PullRequest.PullRequestId";
    public static final String ENV_PR_ID_ALT = "SYSTEM_PULLREQUEST_PULLREQUESTID";
    public static final String ENV_TOKEN = "ADO_TOKEN";
    
    /**
     * Detect Azure DevOps CI environment from environment variables.
     * Returns null if not running in Azure DevOps.
     */
    public static AdoEnvironment detect() {
        var repoName = env(ENV_REPOSITORY_NAME, ENV_REPOSITORY_NAME_ALT);
        if (StringUtils.isBlank(repoName)) return null;
        
        var sourceBranchRaw = env(ENV_SOURCE_BRANCH, ENV_SOURCE_BRANCH_ALT);
        var isPr = StringUtils.isNotBlank(sourceBranchRaw) && sourceBranchRaw.startsWith("refs/pull/");
        var branchInfo = detectBranchInfo(isPr, sourceBranchRaw);
        var sourceBranch = branchInfo[0];
        var targetBranch = branchInfo[1];
        var sha = env(ENV_SOURCE_VERSION, ENV_SOURCE_VERSION_ALT);
        var repositoryId = env(ENV_REPOSITORY_ID, ENV_REPOSITORY_ID_ALT);
        var buildId = env(ENV_BUILD_ID, ENV_BUILD_ID_ALT);
        
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
                () -> EnvHelper.envOrDefault(ENV_SOURCES_DIRECTORY_ALT,
                    () -> EnvHelper.envOrDefault(ENV_DEFAULT_WORKING_DIRECTORY,
                        () -> EnvHelper.envOrDefault(ENV_DEFAULT_WORKING_DIRECTORY_ALT, ".")))))
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
            ? CiPullRequest.active(env(ENV_PR_ID, ENV_PR_ID_ALT), targetBranch)
            : CiPullRequest.inactive();
        
        return AdoEnvironment.builder()
            .organization(env(ENV_ORGANIZATION_URL, ENV_ORGANIZATION_URL_ALT))
            .project(env(ENV_PROJECT, ENV_PROJECT_ALT))
            .repositoryId(repositoryId)
            .buildId(buildId)
            .ciRepository(ciRepository)
            .ciBranch(ciBranch)
            .ciCommit(ciCommit)
            .pullRequest(pullRequest)
            .prTerminology(PR_TERMINOLOGY)
            .prCommentActionSuffix(PR_COMMENT_ACTION_SUFFIX)
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
                () -> EnvHelper.envOrDefault(ENV_PR_SOURCE_BRANCH_ALT,
                    () -> EnvHelper.envOrDefault(ENV_PR_SOURCE_BRANCH_NAME,
                        EnvHelper.env(ENV_PR_SOURCE_BRANCH_NAME_ALT))));
            sourceBranch = StringUtils.isNotBlank(sourceBranch) ? sourceBranch.replaceAll("^refs/heads/", "") : null;
            
            targetBranch = EnvHelper.envOrDefault(ENV_PR_TARGET_BRANCH,
                () -> EnvHelper.envOrDefault(ENV_PR_TARGET_BRANCH_ALT,
                    () -> EnvHelper.envOrDefault(ENV_PR_TARGET_BRANCH_NAME,
                        EnvHelper.env(ENV_PR_TARGET_BRANCH_NAME_ALT))));
            targetBranch = StringUtils.isNotBlank(targetBranch) ? targetBranch.replaceAll("^refs/heads/", "") : null;
        } else {
            sourceBranch = EnvHelper.envOrDefault(ENV_SOURCE_BRANCH_NAME,
                () -> EnvHelper.envOrDefault(ENV_SOURCE_BRANCH_NAME_ALT,
                    StringUtils.isNotBlank(sourceBranchRaw) ? sourceBranchRaw.replaceAll("^refs/heads/", "") : null));
            targetBranch = null;
        }
        
        return new String[]{sourceBranch, targetBranch};
    }

    private static String env(String primaryName, String alternateName) {
        return EnvHelper.envOrDefault(primaryName, () -> EnvHelper.env(alternateName));
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
