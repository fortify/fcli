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

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.ci.CiBranch;
import com.fortify.cli.common.ci.CiCommit;
import com.fortify.cli.common.ci.CiCommitId;
import com.fortify.cli.common.ci.CiPullRequest;
import com.fortify.cli.common.ci.CiRepository;
import com.fortify.cli.common.ci.CiRepositoryName;
import com.fortify.cli.common.json.JsonHelper;
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
    String jobSummaryFile,
    String prTerminology,
    String ciName,
    String ciId
    
) {
    private static final Pattern PR_NUMBER_PATTERN = Pattern.compile("refs/pull/(\\d+)/");
    
    // CI system type identifier
    public static final String TYPE = "github";
    public static final String NAME = "GitHub";
    public static final String ID = "github";
    public static final String PR_TERMINOLOGY = "Pull Request";
    
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
    public static final String ENV_EVENT_PATH = "GITHUB_EVENT_PATH"; // Path to event payload JSON
    
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
        var commitShas = detectCommitShas(isPr);
        
        // Build standardized structures
        var ciRepository = CiRepository.builder()
            .workspaceDir(EnvHelper.envOrDefault(ENV_WORKSPACE, "."))
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
            .headId(CiCommitId.builder()
                .full(commitShas.headSha())
                .short_(StringUtils.isNotBlank(commitShas.headSha()) && commitShas.headSha().length() >= 7 
                    ? commitShas.headSha().substring(0, 7) : commitShas.headSha())
                .build())
            .mergeId(CiCommitId.builder()
                .full(commitShas.mergeSha())
                .short_(StringUtils.isNotBlank(commitShas.mergeSha()) && commitShas.mergeSha().length() >= 7 
                    ? commitShas.mergeSha().substring(0, 7) : commitShas.mergeSha())
                .build())
            .message(null)  // Not available in GitHub Actions environment
            .author(null)   // Not available in GitHub Actions environment
            .committer(null)  // Not available in GitHub Actions environment
            .build();
        
        var pullRequest = isPr 
            ? CiPullRequest.active(prInfo != null ? prInfo.toString() : null, targetBranch)
            : CiPullRequest.inactive();
        
        return GitHubEnvironment.builder()
            .jobSummaryFile(EnvHelper.env(ENV_STEP_SUMMARY))
            .ciRepository(ciRepository)
            .ciBranch(ciBranch)
            .ciCommit(ciCommit)
            .pullRequest(pullRequest)
            .prTerminology(PR_TERMINOLOGY)
            .ciName(NAME)
            .ciId(ID)
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
     * Helper record to hold both head and merge commit SHAs.
     */
    private record CommitShas(String headSha, String mergeSha) {}
    
    /**
     * Detect commit SHAs, differentiating between head and merge commits.
     * For PR events: mergeSha = GITHUB_SHA (merge commit), headSha = pull_request.head.sha (actual PR commit)
     * For push events: both SHAs are the same (GITHUB_SHA)
     * Falls back to GITHUB_SHA for both if head SHA is not available.
     */
    private static CommitShas detectCommitShas(boolean isPr) {
        var githubSha = EnvHelper.env(ENV_SHA);
        
        if (isPr) {
            var headSha = extractHeadShaFromEventPayload();
            if (StringUtils.isNotBlank(headSha)) {
                // For PRs: GITHUB_SHA is merge commit, event payload has head commit
                return new CommitShas(headSha, githubSha);
            }
        }
        
        // For non-PRs or if event payload unavailable: both are the same
        return new CommitShas(githubSha, githubSha);
    }
    
    /**
     * Extract pull_request.head.sha from GitHub event payload JSON.
     * Returns null if event path not available or head SHA not found.
     */
    private static String extractHeadShaFromEventPayload() {
        var eventPath = EnvHelper.env(ENV_EVENT_PATH);
        if (StringUtils.isBlank(eventPath)) return null;
        
        var eventFile = new File(eventPath);
        if (!eventFile.exists() || !eventFile.isFile()) return null;
        
        try {
            var eventPayload = JsonHelper.getObjectMapper().readTree(eventFile);
            return extractHeadShaFromPayload(eventPayload);
        } catch (IOException e) {
            // Silently ignore parsing errors and fall back to GITHUB_SHA
            return null;
        }
    }
    
    /**
     * Extract head SHA from parsed event payload.
     */
    private static String extractHeadShaFromPayload(JsonNode eventPayload) {
        if (eventPayload == null) return null;
        
        var pullRequest = eventPayload.path("pull_request");
        if (pullRequest.isMissingNode()) return null;
        
        var head = pullRequest.path("head");
        if (head.isMissingNode()) return null;
        
        var sha = head.path("sha");
        return sha.isMissingNode() ? null : sha.asText();
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
