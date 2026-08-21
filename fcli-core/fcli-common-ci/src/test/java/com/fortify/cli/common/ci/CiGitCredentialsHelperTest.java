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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class CiGitCredentialsHelperTest {
    @AfterEach
    void clearEnvironment() {
        CiEnvironmentTestHelper.clearAllCiEnvironmentVariables();
        System.clearProperty("fcli.env." + CiGitCredentialsHelper.ENV_GIT_PUSH_TOKEN);
    }

    @Test
    void resolvePushCredentialsUsesGitHubToken() {
        System.setProperty("fcli.env.GITHUB_REPOSITORY", "owner/repo");
        System.setProperty("fcli.env.GITHUB_TOKEN", "gh-token");

        var credentials = CiGitCredentialsHelper.resolvePushCredentials("https://github.com/fortify/fcli.git");

        assertNotNull(credentials);
        assertEquals("x-access-token", credentials.username());
        assertEquals("gh-token", credentials.token());
    }

    @Test
    void resolvePushCredentialsUsesGitLabJobTokenUsername() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "123");
        System.setProperty("fcli.env.CI_JOB_TOKEN", "gl-job-token");

        var credentials = CiGitCredentialsHelper.resolvePushCredentials("https://gitlab.example.com/group/repo.git");

        assertNotNull(credentials);
        assertEquals("gitlab-ci-token", credentials.username());
        assertEquals("gl-job-token", credentials.token());
    }

    @Test
    void resolvePushCredentialsUsesGitLabPatUsername() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "123");
        System.setProperty("fcli.env.GITLAB_TOKEN", "gl-pat");

        var credentials = CiGitCredentialsHelper.resolvePushCredentials("https://gitlab.example.com/group/repo.git");

        assertNotNull(credentials);
        assertEquals("oauth2", credentials.username());
        assertEquals("gl-pat", credentials.token());
    }

    @Test
    void resolvePushCredentialsUsesExplicitOverrideWithDetectedUsername() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "123");
        System.setProperty("fcli.env.CI_JOB_TOKEN", "gl-job-token");
        System.setProperty("fcli.env.GIT_PUSH_TOKEN", "override-token");

        var credentials = CiGitCredentialsHelper.resolvePushCredentials("https://gitlab.example.com/group/repo.git");

        assertNotNull(credentials);
        assertEquals("gitlab-ci-token", credentials.username());
        assertEquals("override-token", credentials.token());
    }

    @Test
    void resolvePushCredentialsReturnsNullForSshRemote() {
        System.setProperty("fcli.env.GITHUB_REPOSITORY", "owner/repo");
        System.setProperty("fcli.env.GITHUB_TOKEN", "gh-token");

        assertNull(CiGitCredentialsHelper.resolvePushCredentials("git@github.com:fortify/fcli.git"));
    }
}