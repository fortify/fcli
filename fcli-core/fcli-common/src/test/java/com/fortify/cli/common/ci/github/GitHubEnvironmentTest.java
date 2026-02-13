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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.common.ci.CiEnvironmentTestHelper;

public class GitHubEnvironmentTest {
    
    @AfterEach
    void clearSystemProperties() {
        CiEnvironmentTestHelper.clearAllCiEnvironmentVariables();
    }
    
    @Test
    void testDetectReturnsNullWhenNotInGitHub() {
        // Clear all CI environment variables to ensure detection returns null even when running in GitHub
        CiEnvironmentTestHelper.clearAllCiEnvironmentVariables();
        var env = GitHubEnvironment.detect();
        assertNull(env);
    }
    
    @Test
    void testDetectRegularCommit() {
        System.setProperty("fcli.env.GITHUB_REPOSITORY", "owner/repo");
        System.setProperty("fcli.env.GITHUB_REF", "refs/heads/main");
        System.setProperty("fcli.env.GITHUB_REF_NAME", "main");
        System.setProperty("fcli.env.GITHUB_SHA", "1234567890abcdef1234567890abcdef12345678");
        System.setProperty("fcli.env.GITHUB_WORKSPACE", "/workspace");
        System.setProperty("fcli.env.GITHUB_STEP_SUMMARY", "/tmp/summary.md");
        
        var env = GitHubEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("/tmp/summary.md", env.jobSummaryFile());
        
        assertNotNull(env.ciRepository());
        assertEquals("/workspace", env.ciRepository().workspaceDir());
        assertEquals("repo", env.ciRepository().name().short_());
        assertEquals("owner/repo", env.ciRepository().name().full());
        
        assertNotNull(env.ciBranch());
        assertEquals("refs/heads/main", env.ciBranch().full());
        assertEquals("main", env.ciBranch().short_());
        
        assertNotNull(env.ciCommit());
        assertEquals("1234567890abcdef1234567890abcdef12345678", env.ciCommit().id().full());
        assertEquals("1234567", env.ciCommit().id().short_());
        
        assertNotNull(env.pullRequest());
        assertEquals(false, env.pullRequest().active());
        assertNull(env.pullRequest().id());
        assertNull(env.pullRequest().target());
    }
    
    @Test
    void testDetectPullRequest() {
        System.setProperty("fcli.env.GITHUB_REPOSITORY", "owner/repo");
        System.setProperty("fcli.env.GITHUB_REF", "refs/pull/123/merge");
        System.setProperty("fcli.env.GITHUB_SHA", "abcdef1234567890abcdef1234567890abcdef12");
        System.setProperty("fcli.env.GITHUB_HEAD_REF", "feature-branch");
        System.setProperty("fcli.env.GITHUB_BASE_REF", "main");
        System.setProperty("fcli.env.GITHUB_WORKSPACE", "/workspace");
        
        var env = GitHubEnvironment.detect();
        
        assertNotNull(env);
        
        assertNotNull(env.ciBranch());
        assertEquals("refs/pull/123/merge", env.ciBranch().full());
        assertEquals("feature-branch", env.ciBranch().short_());
        
        assertNotNull(env.pullRequest());
        assertEquals(true, env.pullRequest().active());
        assertEquals("123", env.pullRequest().id());
        assertEquals("main", env.pullRequest().target());
    }
    
    @Test
    void testGetQualifiedRepoName() {
        System.setProperty("fcli.env.GITHUB_REPOSITORY", "owner/repo");
        System.setProperty("fcli.env.GITHUB_REF", "refs/heads/develop");
        System.setProperty("fcli.env.GITHUB_REF_NAME", "develop");
        System.setProperty("fcli.env.GITHUB_SHA", "abc123");
        
        var env = GitHubEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("owner/repo:develop", env.getQualifiedRepoName());
    }
    
    @Test
    void testGetBranchForVersioning() {
        System.setProperty("fcli.env.GITHUB_REPOSITORY", "owner/repo");
        System.setProperty("fcli.env.GITHUB_REF", "refs/heads/release-1.0");
        System.setProperty("fcli.env.GITHUB_REF_NAME", "release-1.0");
        System.setProperty("fcli.env.GITHUB_SHA", "abc123");
        
        var env = GitHubEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("release-1.0", env.getBranchForVersioning());
    }
    
    @Test
    void testGetBranchForVersioningInPullRequest() {
        System.setProperty("fcli.env.GITHUB_REPOSITORY", "owner/repo");
        System.setProperty("fcli.env.GITHUB_REF", "refs/pull/456/merge");
        System.setProperty("fcli.env.GITHUB_SHA", "abc123");
        System.setProperty("fcli.env.GITHUB_HEAD_REF", "feature-x");
        System.setProperty("fcli.env.GITHUB_BASE_REF", "main");
        
        var env = GitHubEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("feature-x", env.getBranchForVersioning());
    }
    
    @Test
    void testShortCommitIdTruncation() {
        System.setProperty("fcli.env.GITHUB_REPOSITORY", "owner/repo");
        System.setProperty("fcli.env.GITHUB_REF", "refs/heads/main");
        System.setProperty("fcli.env.GITHUB_SHA", "abc");
        
        var env = GitHubEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("abc", env.ciCommit().id().full());
        assertEquals("abc", env.ciCommit().id().short_());
    }
}
