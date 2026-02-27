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
package com.fortify.cli.common.ci.bitbucket;

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
 * Immutable record holding detected Bitbucket Pipelines environment data.
 * Provides context-aware branch detection for regular commits, pull requests,
 * and tag pipelines. Produces standardized nested structures matching the
 * {@code localRepo()} SpEL function output.
 */
@Reflectable
@Builder
public record BitbucketEnvironment(
    CiRepository ciRepository,
    CiBranch ciBranch,
    CiCommit ciCommit,
    CiPullRequest pullRequest,
    String workspace,
    String repositoryOwner,
    String repositorySlug,
    String repositoryFullName,
    String pipelineUuid,
    String prTerminology,
    String ciName,
    String ciId
) {
    public static final String TYPE = "bitbucket";
    public static final String NAME = "Bitbucket";
    public static final String ID = "bitbucket";
    public static final String PR_TERMINOLOGY = "Pull Request";

    public static final String ENV_WORKSPACE = "BITBUCKET_WORKSPACE";
    public static final String ENV_REPO_OWNER = "BITBUCKET_REPO_OWNER";
    public static final String ENV_REPO_SLUG = "BITBUCKET_REPO_SLUG";
    public static final String ENV_REPO_FULL_NAME = "BITBUCKET_REPO_FULL_NAME";
    public static final String ENV_BRANCH = "BITBUCKET_BRANCH";
    public static final String ENV_TAG = "BITBUCKET_TAG";
    public static final String ENV_COMMIT = "BITBUCKET_COMMIT";
    public static final String ENV_CLONE_DIR = "BITBUCKET_CLONE_DIR";
    public static final String ENV_GIT_HTTP_ORIGIN = "BITBUCKET_GIT_HTTP_ORIGIN";
    public static final String ENV_GIT_SSH_ORIGIN = "BITBUCKET_GIT_SSH_ORIGIN";
    public static final String ENV_PR_ID = "BITBUCKET_PR_ID";
    public static final String ENV_PR_DEST_BRANCH = "BITBUCKET_PR_DESTINATION_BRANCH";
    public static final String ENV_PIPELINE_UUID = "BITBUCKET_PIPELINE_UUID";
    public static final String ENV_API_URL = "BITBUCKET_API_URL";
    public static final String ENV_TOKEN = "BITBUCKET_TOKEN";
    public static final String ENV_STEP_OAUTH_TOKEN = "BITBUCKET_STEP_OAUTH_ACCESS_TOKEN";
    public static final String ENV_USERNAME = "BITBUCKET_USERNAME";
    public static final String ENV_APP_PASSWORD = "BITBUCKET_APP_PASSWORD";

    public static BitbucketEnvironment detect() {
        var repoSlug = EnvHelper.env(ENV_REPO_SLUG);
        var workspace = EnvHelper.env(ENV_WORKSPACE);
        var repoOwner = EnvHelper.env(ENV_REPO_OWNER);
        var repoFullNameRaw = EnvHelper.env(ENV_REPO_FULL_NAME);

        if (StringUtils.isBlank(repoSlug)) {
            repoSlug = extractSlugFromFullName(repoFullNameRaw);
        }
        if (StringUtils.isBlank(repoSlug)) {
            return null;
        }

        var namespace = determineNamespace(workspace, repoOwner, repoFullNameRaw);
        var repoFullName = determineFullName(repoFullNameRaw, namespace, repoSlug);
        var branch = EnvHelper.env(ENV_BRANCH);
        var tag = EnvHelper.env(ENV_TAG);
        var commitSha = EnvHelper.env(ENV_COMMIT);
        var workDir = EnvHelper.envOrDefault(ENV_CLONE_DIR, ".");
        var remoteUrl = StringUtils.firstNonBlank(
            EnvHelper.env(ENV_GIT_HTTP_ORIGIN),
            EnvHelper.env(ENV_GIT_SSH_ORIGIN));

        var ciRepository = CiRepository.builder()
            .workspaceDir(workDir)
            .remoteUrl(remoteUrl)
            .name(CiRepositoryName.builder()
                .short_(repoSlug)
                .full(repoFullName)
                .build())
            .build();

        var prId = EnvHelper.env(ENV_PR_ID);
        var ciBranch = CiBranch.builder()
            .full(buildFullRef(branch, tag, prId))
            .short_(StringUtils.isNotBlank(branch) ? branch : tag)
            .build();

        var ciCommit = CiCommit.builder()
            .headId(CiCommitId.builder()
                .full(commitSha)
                .short_(shortSha(commitSha))
                .build())
            .mergeId(CiCommitId.builder()
                .full(commitSha)
                .short_(shortSha(commitSha))
                .build())
            .message(null)
            .author(null)
            .committer(null)
            .build();

        var pullRequest = StringUtils.isNotBlank(prId)
            ? CiPullRequest.active(prId, EnvHelper.env(ENV_PR_DEST_BRANCH))
            : CiPullRequest.inactive();

        return BitbucketEnvironment.builder()
            .ciRepository(ciRepository)
            .ciBranch(ciBranch)
            .ciCommit(ciCommit)
            .pullRequest(pullRequest)
            .workspace(namespace)
            .repositoryOwner(repoOwner)
            .repositorySlug(repoSlug)
            .repositoryFullName(repoFullName)
            .pipelineUuid(EnvHelper.env(ENV_PIPELINE_UUID))
            .prTerminology(PR_TERMINOLOGY)
            .ciName(NAME)
            .ciId(ID)
            .build();
    }

    public String getQualifiedRepoName() {
        var branch = ciBranch != null ? ciBranch.short_() : null;
        return StringUtils.isNotBlank(branch) && StringUtils.isNotBlank(repositoryFullName)
            ? repositoryFullName + ":" + branch
            : repositoryFullName;
    }

    public String getBranchForVersioning() {
        return ciBranch != null ? ciBranch.short_() : null;
    }

    private static String determineFullName(String explicitFullName, String namespace, String slug) {
        if (StringUtils.isNotBlank(explicitFullName)) {
            return explicitFullName;
        }
        if (StringUtils.isNotBlank(namespace) && StringUtils.isNotBlank(slug)) {
            return namespace + "/" + slug;
        }
        return slug;
    }

    private static String extractSlugFromFullName(String fullName) {
        if (StringUtils.isBlank(fullName) || !fullName.contains("/")) {
            return null;
        }
        return fullName.substring(fullName.indexOf('/') + 1);
    }

    private static String determineNamespace(String workspace, String repoOwner, String fullName) {
        var namespace = StringUtils.firstNonBlank(workspace, repoOwner);
        if (StringUtils.isNotBlank(namespace)) {
            return namespace;
        }
        if (StringUtils.isNotBlank(fullName) && fullName.contains("/")) {
            return fullName.substring(0, fullName.indexOf('/'));
        }
        return null;
    }

    private static String shortSha(String commitSha) {
        if (StringUtils.isBlank(commitSha)) {
            return commitSha;
        }
        return commitSha.length() >= 7 ? commitSha.substring(0, 7) : commitSha;
    }

    private static String buildFullRef(String branch, String tag, String prId) {
        if (StringUtils.isNotBlank(prId)) {
            return "refs/pull-requests/" + prId + "/merge";
        }
        if (StringUtils.isNotBlank(branch)) {
            return "refs/heads/" + branch;
        }
        if (StringUtils.isNotBlank(tag)) {
            return "refs/tags/" + tag;
        }
        return null;
    }
}
