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
package com.fortify.cli.common.ci.gitlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.common.ci.CiEnvironmentTestHelper;

public class GitLabEnvironmentTest {
    
    @AfterEach
    void clearSystemProperties() {
        CiEnvironmentTestHelper.clearAllCiEnvironmentVariables();
    }
    
    @Test
    void testDetectReturnsNullWhenNotInGitLab() {
        // Clear all CI environment variables to ensure detection returns null even when running in GitLab
        CiEnvironmentTestHelper.clearAllCiEnvironmentVariables();
        var env = GitLabEnvironment.detect();
        assertNull(env);
    }
    
    @Test
    void testDetectRegularCommit() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "12345");
        System.setProperty("fcli.env.CI_PROJECT_NAME", "myproject");
        System.setProperty("fcli.env.CI_PROJECT_PATH", "group/myproject");
        System.setProperty("fcli.env.CI_PROJECT_DIR", "/builds/project");
        System.setProperty("fcli.env.CI_COMMIT_SHA", "fedcba0987654321fedcba0987654321fedcba09");
        System.setProperty("fcli.env.CI_COMMIT_BRANCH", "develop");
        System.setProperty("fcli.env.CI_PIPELINE_ID", "9876");
        System.setProperty("fcli.env.CI_REPOSITORY_URL", "https://gitlab.example.com/group/myproject.git");
        
        var env = GitLabEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("12345", env.projectId());
        assertEquals("9876", env.pipelineId());
        
        assertNotNull(env.ciRepository());
        assertEquals("/builds/project", env.ciRepository().workspaceDir());
        assertEquals("https://gitlab.example.com/group/myproject.git", env.ciRepository().remoteUrl());
        assertEquals("myproject", env.ciRepository().name().short_());
        assertEquals("group/myproject", env.ciRepository().name().full());
        
        assertNotNull(env.ciBranch());
        assertEquals("refs/heads/develop", env.ciBranch().full());
        assertEquals("develop", env.ciBranch().short_());
        
        assertNotNull(env.ciCommit());
        assertEquals("fedcba0987654321fedcba0987654321fedcba09", env.ciCommit().id().full());
        assertEquals("fedcba0", env.ciCommit().id().short_());
        
        assertNotNull(env.pullRequest());
        assertEquals(false, env.pullRequest().active());
        assertNull(env.pullRequest().id());
        assertNull(env.pullRequest().target());
    }
    
    @Test
    void testDetectMergeRequest() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "12345");
        System.setProperty("fcli.env.CI_PROJECT_NAME", "myproject");
        System.setProperty("fcli.env.CI_PROJECT_PATH", "group/myproject");
        System.setProperty("fcli.env.CI_PROJECT_DIR", "/builds/project");
        System.setProperty("fcli.env.CI_COMMIT_SHA", "abc1234567890def");
        System.setProperty("fcli.env.CI_MERGE_REQUEST_IID", "42");
        System.setProperty("fcli.env.CI_MERGE_REQUEST_SOURCE_BRANCH_NAME", "feature-branch");
        System.setProperty("fcli.env.CI_MERGE_REQUEST_TARGET_BRANCH_NAME", "main");
        
        var env = GitLabEnvironment.detect();
        
        assertNotNull(env);
        
        assertNotNull(env.ciBranch());
        assertEquals("refs/merge-requests/42/head", env.ciBranch().full());
        assertEquals("feature-branch", env.ciBranch().short_());
        
        assertNotNull(env.pullRequest());
        assertEquals(true, env.pullRequest().active());
        assertEquals("42", env.pullRequest().id());
        assertEquals("main", env.pullRequest().target());
    }
    
    @Test
    void testGetQualifiedRepoName() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "123");
        System.setProperty("fcli.env.CI_PROJECT_NAME", "myproject");
        System.setProperty("fcli.env.CI_PROJECT_PATH", "org/team/myproject");
        System.setProperty("fcli.env.CI_COMMIT_SHA", "abc123");
        System.setProperty("fcli.env.CI_COMMIT_BRANCH", "feature");
        
        var env = GitLabEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("org/team/myproject:feature", env.getQualifiedRepoName());
    }
    
    @Test
    void testGetBranchForVersioning() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "123");
        System.setProperty("fcli.env.CI_PROJECT_NAME", "myproject");
        System.setProperty("fcli.env.CI_PROJECT_PATH", "org/myproject");
        System.setProperty("fcli.env.CI_COMMIT_SHA", "abc123");
        System.setProperty("fcli.env.CI_COMMIT_BRANCH", "staging");
        
        var env = GitLabEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("staging", env.getBranchForVersioning());
    }
    
    @Test
    void testGetBranchForVersioningInMergeRequest() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "123");
        System.setProperty("fcli.env.CI_PROJECT_NAME", "myproject");
        System.setProperty("fcli.env.CI_PROJECT_PATH", "org/myproject");
        System.setProperty("fcli.env.CI_COMMIT_SHA", "abc123");
        System.setProperty("fcli.env.CI_MERGE_REQUEST_IID", "99");
        System.setProperty("fcli.env.CI_MERGE_REQUEST_SOURCE_BRANCH_NAME", "hotfix");
        System.setProperty("fcli.env.CI_MERGE_REQUEST_TARGET_BRANCH_NAME", "production");
        
        var env = GitLabEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("hotfix", env.getBranchForVersioning());
    }
    
    @Test
    void testDetectProjectPathFromUrl() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "123");
        System.setProperty("fcli.env.CI_PROJECT_NAME", "myproject");
        System.setProperty("fcli.env.CI_REPOSITORY_URL", "https://gitlab.com/acme/products/myproject.git");
        System.setProperty("fcli.env.CI_COMMIT_SHA", "abc123");
        System.setProperty("fcli.env.CI_COMMIT_BRANCH", "main");
        
        var env = GitLabEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("acme/products/myproject", env.ciRepository().name().full());
    }
    
    @Test
    void testNullPipelineId() {
        System.setProperty("fcli.env.GITLAB_CI", "true");
        System.setProperty("fcli.env.CI_PROJECT_ID", "123");
        System.setProperty("fcli.env.CI_PROJECT_NAME", "myproject");
        System.setProperty("fcli.env.CI_PROJECT_PATH", "org/myproject");
        System.setProperty("fcli.env.CI_COMMIT_SHA", "abc123");
        System.setProperty("fcli.env.CI_COMMIT_BRANCH", "main");
        
        var env = GitLabEnvironment.detect();
        
        assertNotNull(env);
        assertNull(env.pipelineId());
    }
}
