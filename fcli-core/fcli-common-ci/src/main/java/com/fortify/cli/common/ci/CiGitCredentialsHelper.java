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
 * Resolves the token to use for authenticated Git operations (in particular {@code push}).
 * The generic {@code GIT_PUSH_TOKEN} environment variable takes precedence, as CI-provided
 * tokens may not carry push permissions. If not set, this falls back to the token detected
 * for the currently active CI system, reusing the token detection already implemented in the
 * individual {@code *Environment} classes.
 */
public final class CiGitCredentialsHelper {
    /** Generic, CI-agnostic push token override. */
    public static final String ENV_GIT_PUSH_TOKEN = "GIT_PUSH_TOKEN";

    private CiGitCredentialsHelper() {}

    /**
     * Resolve the token to use for pushing to the remote repository.
     *
     * @return {@code GIT_PUSH_TOKEN} if set, otherwise the token detected for the active CI
     *         system, or {@code null} if neither is available.
     */
    public static String resolvePushToken() {
        var explicitToken = EnvHelper.env(ENV_GIT_PUSH_TOKEN);
        return StringUtils.isNotBlank(explicitToken) ? explicitToken : activeCiSystemToken();
    }

    private static String activeCiSystemToken() {
        if (GitHubEnvironment.detect() != null) { return EnvHelper.env(GitHubEnvironment.ENV_TOKEN); }
        if (GitLabEnvironment.detect() != null) { return EnvHelper.env(GitLabEnvironment.ENV_TOKEN); }
        if (AdoEnvironment.detect() != null) { return EnvHelper.env(AdoEnvironment.ENV_TOKEN); }
        if (BitbucketEnvironment.detect() != null) {
            return EnvHelper.env(BitbucketEnvironment.ENV_TOKEN, BitbucketEnvironment.ENV_STEP_OAUTH_TOKEN);
        }
        return null;
    }
}
