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
package com.fortify.cli.common.ci.github;

import java.util.regex.Pattern;

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
 * Immutable record holding detected GitHub Actions environment data.
 * Provides context-aware branch detection for both regular commits and pull requests.
 * 
 * <p>This class provides standardized nested structures ({@link CiRepository}, {@link CiBranch}, 
 * {@link CiCommit}) that match the format returned by the {@code localRepo()} SpEL function,
 * plus GitHub-specific properties.
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public record GitHubEnvironment(
    // Standardized nested structures matching localRepo() format
    CiRepository ciRepository,
    CiBranch ciBranch,
    CiCommit ciCommit,
    CiPullRequest pullRequest,
    // GitHub-specific properties
    String jobSummaryFile
    
) {
    private static final Pattern PR_NUMBER_PATTERN = Pattern.compile("refs/pull/(\\d+)/");
    
    // CI system type identifier
    public static final String TYPE = "github";
    
    // Environment variable names
    public static final String ENV_REPOSITORY = "GITHUB_REPOSITORY";
    public static final String ENV_REF = "GITHUB_REF";
    public static final String ENV_REF_NAME = "GITHUB_REF_NAME";
    public static final String ENV_SHA = "GITHUB_SHA";
    public static final String ENV_HEAD_REF = "GITHUB_HEAD_REF";
    public static final String ENV_BASE_REF = "GITHUB_BASE_REF";
    public static final String ENV_WORKSPACE = "GITHUB_WORKSPACE";
    public static final String ENV_STEP_SUMMARY = "GITHUB_STEP_SUMMARY";
    public static final String ENV_SERVER_URL = "GITHUB_SERVER_URL"; // Base URL for GitHub Enterprise
    public static final String ENV_API_URL = "GITHUB_API_URL"; // API URL for GitHub Enterprise
    public static final String ENV_TOKEN = "GITHUB_TOKEN";
    
    /**
     * Detect GitHub Actions environment from environment variables.
     * Returns null if not running in GitHub Actions.
     */
    public static GitHubEnvironment detect() {
        var ghRepo = EnvHelper.env(ENV_REPOSITORY);
        if (StringUtils.isBlank(ghRepo)) return null;
        
        var ref = EnvHelper.env(ENV_REF);
        var isPr = isPullRequest(ref);
        var branchInfo = detectBranchInfo(ref, isPr);
        var prInfo = isPr ? detectPullRequestInfo(ref) : null;
        var repoParts = ghRepo.split("/", 2);
        
        var repo = repoParts.length > 1 ? repoParts[1] : ghRepo;
        var sourceBranch = branchInfo[0];
        var targetBranch = branchInfo[1];
        var sha = EnvHelper.env(ENV_SHA);
        
        // Build standardized structures
        var ciRepository = CiRepository.builder()
            .workDir(EnvHelper.envOrDefault(ENV_WORKSPACE, "."))
            .remoteUrl(null)  // Could be constructed from server URL, but typically not needed
            .name(CiRepositoryName.builder()
                .short_(repo)
                .full(ghRepo)
                .build())
            .build();
        
        var ciBranch = CiBranch.builder()
            .full(ref)
            .short_(sourceBranch)
            .build();
        
        var ciCommit = CiCommit.builder()
            .id(CiCommitId.builder()
                .full(sha)
                .short_(StringUtils.isNotBlank(sha) && sha.length() >= 7 ? sha.substring(0, 7) : sha)
                .build())
            .message(null)  // Not available in GitHub Actions environment
            .author(null)   // Not available in GitHub Actions environment
            .committer(null)  // Not available in GitHub Actions environment
            .build();
        
        var pullRequest = isPr 
            ? CiPullRequest.active(prInfo, targetBranch)
            : CiPullRequest.inactive();
        
        return GitHubEnvironment.builder()
            .jobSummaryFile(EnvHelper.env(ENV_STEP_SUMMARY))
            .ciRepository(ciRepository)
            .ciBranch(ciBranch)
            .ciCommit(ciCommit)
            .pullRequest(pullRequest)
            .build();
    }
    
    private static boolean isPullRequest(String ref) {
        return StringUtils.isNotBlank(ref) && ref.startsWith("refs/pull/");
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
        if (StringUtils.isBlank(ref)) return null;
        var matcher = PR_NUMBER_PATTERN.matcher(ref);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }
    
    /**
     * Get qualified repository name for Fortify (owner/repo:branch format).
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
