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
package com.fortify.cli.common.ci;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.ci.ado.AdoEnvironment;
import com.fortify.cli.common.ci.bitbucket.BitbucketEnvironment;
import com.fortify.cli.common.ci.github.GitHubEnvironment;
import com.fortify.cli.common.ci.gitlab.GitLabEnvironment;
import com.fortify.cli.common.util.EnvHelper;

/**
 * Resolves basic-auth credentials for authenticated Git operations (in particular {@code push}).
 * The generic {@code GIT_PUSH_TOKEN} environment variable takes precedence, as CI-provided
 * tokens may not carry push permissions. If not set, this falls back to the credentials detected
 * for the currently active CI system, reusing the token detection already implemented in the
 * individual {@code *Environment} classes.
 */
public final class CiGitCredentialsHelper {
    /** Generic, CI-agnostic push token override. */
    public static final String ENV_GIT_PUSH_TOKEN = "GIT_PUSH_TOKEN";

    private static final String GITHUB_USERNAME = "x-access-token";
    private static final String GITLAB_PAT_USERNAME = "oauth2";
    private static final String GITLAB_JOB_TOKEN_USERNAME = "gitlab-ci-token";
    private static final String ADO_USERNAME = "AzureDevOps";
    private static final String BITBUCKET_TOKEN_USERNAME = "x-token-auth";

    private CiGitCredentialsHelper() {}

    /**
     * Resolve the basic-auth credentials to use for pushing to the remote repository.
     *
     * @param remoteUrl remote URL of the git repository, used to infer a username when only a
     *                  generic push token override is available
     * @return credentials for push operations, or {@code null} if no credentials are available
     */
    public static CiGitCredentials resolvePushCredentials(String remoteUrl) {
        if (isSshUrl(remoteUrl)) {
            return null;
        }

        var explicitToken = EnvHelper.env(ENV_GIT_PUSH_TOKEN);
        if (StringUtils.isNotBlank(explicitToken)) {
            return new CiGitCredentials(resolveUsername(remoteUrl), explicitToken);
        }
        return resolveActiveCiCredentials();
    }

    private static CiGitCredentials resolveActiveCiCredentials() {
        if (GitHubEnvironment.detect() != null) {
            return credentials(GITHUB_USERNAME, EnvHelper.env(GitHubEnvironment.ENV_TOKEN));
        }
        if (GitLabEnvironment.detect() != null) {
            var token = EnvHelper.env(GitLabEnvironment.ENV_TOKEN);
            if (StringUtils.isBlank(token)) {
                return null;
            }
            var jobToken = EnvHelper.env(GitLabEnvironment.ENV_JOB_TOKEN);
            var username = StringUtils.isNotBlank(jobToken) && jobToken.equals(token)
                    ? GITLAB_JOB_TOKEN_USERNAME : GITLAB_PAT_USERNAME;
            return credentials(username, token);
        }
        if (AdoEnvironment.detect() != null) {
            return credentials(ADO_USERNAME, EnvHelper.env(AdoEnvironment.ENV_TOKEN));
        }
        if (BitbucketEnvironment.detect() != null) {
            var username = EnvHelper.env(BitbucketEnvironment.ENV_USERNAME);
            var token = EnvHelper.env(BitbucketEnvironment.ENV_APP_PASSWORD);
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(token)) {
                return credentials(username, token);
            }
            token = EnvHelper.env(BitbucketEnvironment.ENV_STEP_OAUTH_TOKEN);
            if (StringUtils.isBlank(token)) {
                token = EnvHelper.env(BitbucketEnvironment.ENV_TOKEN);
            }
            return credentials(BITBUCKET_TOKEN_USERNAME, token);
        }
        return null;
    }

    private static CiGitCredentials credentials(String username, String token) {
        return StringUtils.isNotBlank(token) ? new CiGitCredentials(username, token) : null;
    }

    private static String resolveUsername(String remoteUrl) {
        var activeCredentials = resolveActiveCiCredentials();
        if (activeCredentials != null && StringUtils.isNotBlank(activeCredentials.username())) {
            return activeCredentials.username();
        }
        switch (detectPlatformFromUrl(remoteUrl)) {
            case "github":
                return GITHUB_USERNAME;
            case "gitlab":
                return GITLAB_PAT_USERNAME;
            case "ado":
                return ADO_USERNAME;
            case "bitbucket":
                return BITBUCKET_TOKEN_USERNAME;
            default:
                return "git";
        }
    }

    private static boolean isSshUrl(String remoteUrl) {
        if (StringUtils.isBlank(remoteUrl)) {
            return false;
        }
        var url = remoteUrl.trim();
        return url.startsWith("git@") || url.startsWith("ssh://");
    }

    private static String detectPlatformFromUrl(String remoteUrl) {
        if (StringUtils.isBlank(remoteUrl)) {
            return "unknown";
        }
        try {
            String host;
            var cleaned = remoteUrl.trim();
            if (cleaned.startsWith("git@")) {
                int colon = cleaned.indexOf(':');
                int at = cleaned.indexOf('@');
                host = (at >= 0 && colon > at) ? cleaned.substring(at + 1, colon) : null;
            } else {
                host = java.net.URI.create(cleaned).getHost();
            }
            if (host == null) {
                return "unknown";
            }
            host = host.toLowerCase();
            if (host.equals("github.com") || host.endsWith(".github.com")) {
                return "github";
            }
            if (host.equals("gitlab.com") || host.contains("gitlab")) {
                return "gitlab";
            }
            if (host.contains("dev.azure.com") || host.contains("visualstudio.com") || host.contains("azure")) {
                return "ado";
            }
            if (host.contains("bitbucket")) {
                return "bitbucket";
            }
        } catch (Exception e) {
            return "unknown";
        }
        return "unknown";
    }
}
