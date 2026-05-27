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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.common.ci.CiEnvironmentTestHelper;

class BitbucketEnvironmentTest {
    @AfterEach
    void clearSystemProperties() {
        CiEnvironmentTestHelper.clearAllCiEnvironmentVariables();
    }

    @Test
    void testDetectReturnsNullOutsideBitbucket() {
        CiEnvironmentTestHelper.clearAllCiEnvironmentVariables();
        assertNull(BitbucketEnvironment.detect());
    }

    @Test
    void testDetectRegularBranchBuild() {
        System.setProperty("fcli.env.BITBUCKET_REPO_SLUG", "awesome-repo");
        System.setProperty("fcli.env.BITBUCKET_REPO_OWNER", "acme");
        System.setProperty("fcli.env.BITBUCKET_BRANCH", "main");
        System.setProperty("fcli.env.BITBUCKET_COMMIT", "1234567890abcdef1234567890abcdef12345678");
        System.setProperty("fcli.env.BITBUCKET_CLONE_DIR", "/opt/build");
        System.setProperty("fcli.env.BITBUCKET_GIT_HTTP_ORIGIN", "https://bitbucket.org/acme/awesome-repo.git");

        var env = BitbucketEnvironment.detect();

        assertNotNull(env);
        assertEquals("acme/awesome-repo", env.repositoryFullName());
        assertEquals("main", env.ciBranch().short_());
        assertEquals("refs/heads/main", env.ciBranch().full());
        assertEquals("1234567", env.ciCommit().headId().short_());
        assertEquals("1234567890abcdef1234567890abcdef12345678", env.ciCommit().mergeId().full());
        // For Bitbucket, headId and mergeId are always the same
        assertEquals(false, env.pullRequest().active());
        assertEquals("/opt/build", env.ciRepository().workspaceDir());
        assertEquals("https://bitbucket.org/acme/awesome-repo.git", env.ciRepository().remoteUrl());
    }

    @Test
    void testDetectPullRequest() {
        System.setProperty("fcli.env.BITBUCKET_REPO_FULL_NAME", "acme/security-repo");
        System.setProperty("fcli.env.BITBUCKET_BRANCH", "feature/sast");
        System.setProperty("fcli.env.BITBUCKET_COMMIT", "abcdef1234567890abcdef1234567890abcdef12");
        System.setProperty("fcli.env.BITBUCKET_PR_ID", "42");
        System.setProperty("fcli.env.BITBUCKET_PR_DESTINATION_BRANCH", "main");

        var env = BitbucketEnvironment.detect();

        assertNotNull(env);
        assertEquals("refs/pull-requests/42/merge", env.ciBranch().full());
        assertEquals(true, env.pullRequest().active());
        assertEquals("42", env.pullRequest().id());
        assertEquals("main", env.pullRequest().target());
    }

    @Test
    void testQualifiedRepoName() {
        System.setProperty("fcli.env.BITBUCKET_REPO_SLUG", "infra");
        System.setProperty("fcli.env.BITBUCKET_WORKSPACE", "platform");
        System.setProperty("fcli.env.BITBUCKET_BRANCH", "release");
        System.setProperty("fcli.env.BITBUCKET_COMMIT", "abc123");

        var env = BitbucketEnvironment.detect();

        assertNotNull(env);
        assertEquals("platform/infra:release", env.getQualifiedRepoName());
    }

    @Test
    void testBranchFallbackToTag() {
        System.setProperty("fcli.env.BITBUCKET_REPO_SLUG", "mobile");
        System.setProperty("fcli.env.BITBUCKET_WORKSPACE", "apps");
        System.setProperty("fcli.env.BITBUCKET_TAG", "v1.2.3");
        System.setProperty("fcli.env.BITBUCKET_COMMIT", "abc123");

        var env = BitbucketEnvironment.detect();

        assertNotNull(env);
        assertEquals("refs/tags/v1.2.3", env.ciBranch().full());
        assertEquals("v1.2.3", env.getBranchForVersioning());
    }
}
