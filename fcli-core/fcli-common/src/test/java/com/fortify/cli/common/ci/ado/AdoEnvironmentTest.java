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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.common.ci.CiEnvironmentTestHelper;

public class AdoEnvironmentTest {
    
    @AfterEach
    void clearSystemProperties() {
        CiEnvironmentTestHelper.clearAllCiEnvironmentVariables();
    }
    
    @Test
    void testDetectReturnsNullWhenNotInAdo() {
        // Clear all CI environment variables to ensure detection returns null even when running in ADO
        CiEnvironmentTestHelper.clearAllCiEnvironmentVariables();
        var env = AdoEnvironment.detect();
        assertNull(env);
    }
    
    @Test
    void testDetectRegularCommit() {

        System.setProperty("fcli.env.System.TeamFoundationCollectionUri", "https://dev.azure.com/myorg/");
        System.setProperty("fcli.env.System.TeamProject", "MyProject");
        System.setProperty("fcli.env.Build.Repository.Name", "MyRepo");
        System.setProperty("fcli.env.Build.Repository.ID", "11111111-2222-3333-4444-555555555555");
        System.setProperty("fcli.env.Build.BuildId", "101");
        System.setProperty("fcli.env.Build.SourceBranch", "refs/heads/main");
        System.setProperty("fcli.env.Build.SourceBranchName", "main");
        System.setProperty("fcli.env.Build.SourceVersion", "9876543210abcdef9876543210abcdef98765432");
        System.setProperty("fcli.env.Build.SourcesDirectory", "/home/vsts/work/1/s");
        
        var env = AdoEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("https://dev.azure.com/myorg/", env.organization());
        assertEquals("MyProject", env.project());
        assertEquals("11111111-2222-3333-4444-555555555555", env.repositoryId());
        assertEquals(101, env.buildId());
        
        assertNotNull(env.ciRepository());
        assertEquals("/home/vsts/work/1/s", env.ciRepository().workDir());
        assertEquals("MyRepo", env.ciRepository().name().short_());
        assertEquals("MyRepo", env.ciRepository().name().full());
        
        assertNotNull(env.ciBranch());
        assertEquals("refs/heads/main", env.ciBranch().full());
        assertEquals("main", env.ciBranch().short_());
        
        assertNotNull(env.ciCommit());
        assertEquals("9876543210abcdef9876543210abcdef98765432", env.ciCommit().id().full());
        assertEquals("9876543", env.ciCommit().id().short_());
        
        assertNotNull(env.pullRequest());
        assertEquals(false, env.pullRequest().active());
        assertNull(env.pullRequest().id());
        assertNull(env.pullRequest().target());
    }
    
    @Test
    void testDetectPullRequest() {
        System.setProperty("fcli.env.System.TeamFoundationCollectionUri", "https://dev.azure.com/myorg/");
        System.setProperty("fcli.env.System.TeamProject", "MyProject");
        System.setProperty("fcli.env.Build.Repository.Name", "MyRepo");
        System.setProperty("fcli.env.Build.Repository.ID", "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        System.setProperty("fcli.env.Build.BuildId", "202");
        System.setProperty("fcli.env.Build.SourceBranch", "refs/pull/123/merge");
        System.setProperty("fcli.env.Build.SourceVersion", "abcdef1234567890");
        System.setProperty("fcli.env.System.PullRequest.SourceBranch", "refs/heads/feature-xyz");
        System.setProperty("fcli.env.System.PullRequest.SourceBranchName", "feature-xyz");
        System.setProperty("fcli.env.System.PullRequest.TargetBranch", "refs/heads/develop");
        System.setProperty("fcli.env.System.PullRequest.TargetBranchName", "develop");
        System.setProperty("fcli.env.System.PullRequest.PullRequestId", "123");
        System.setProperty("fcli.env.Build.SourcesDirectory", "/home/vsts/work/1/s");
        
        var env = AdoEnvironment.detect();
        
        assertNotNull(env);
        
        assertNotNull(env.ciBranch());
        assertEquals("refs/pull/123/merge", env.ciBranch().full());
        assertEquals("feature-xyz", env.ciBranch().short_());
        
        assertNotNull(env.pullRequest());
        assertEquals(true, env.pullRequest().active());
        assertEquals(123, env.pullRequest().id());
        assertEquals("develop", env.pullRequest().target());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", env.repositoryId());
        assertEquals(202, env.buildId());
    }
    
    @Test
    void testDetectPullRequestAlternativeEnvVars() {
        System.setProperty("fcli.env.System.TeamFoundationCollectionUri", "https://dev.azure.com/myorg/");
        System.setProperty("fcli.env.System.TeamProject", "MyProject");
        System.setProperty("fcli.env.Build.Repository.Name", "MyRepo");
        System.setProperty("fcli.env.Build.SourceBranch", "refs/pull/456/merge");
        System.setProperty("fcli.env.Build.SourceVersion", "def123");
        System.setProperty("fcli.env.System.PullRequest.SourceBranchName", "bugfix-abc");
        System.setProperty("fcli.env.System.PullRequest.TargetBranchName", "release");
        System.setProperty("fcli.env.System.PullRequest.PullRequestId", "456");
        System.setProperty("fcli.env.Build.SourcesDirectory", "/work");
        
        var env = AdoEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("bugfix-abc", env.ciBranch().short_());
        assertEquals("release", env.pullRequest().target());
    }
    
    @Test
    void testGetQualifiedRepoName() {
        System.setProperty("fcli.env.Build.Repository.Name", "ProductRepo");
        System.setProperty("fcli.env.Build.SourceBranch", "refs/heads/staging");
        System.setProperty("fcli.env.Build.SourceBranchName", "staging");
        System.setProperty("fcli.env.Build.SourceVersion", "abc123");
        
        var env = AdoEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("ProductRepo:staging", env.getQualifiedRepoName());
    }
    
    @Test
    void testGetBranchForVersioning() {
        System.setProperty("fcli.env.Build.Repository.Name", "ProductRepo");
        System.setProperty("fcli.env.Build.SourceBranch", "refs/heads/release-2.0");
        System.setProperty("fcli.env.Build.SourceBranchName", "release-2.0");
        System.setProperty("fcli.env.Build.SourceVersion", "abc123");
        
        var env = AdoEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("release-2.0", env.getBranchForVersioning());
    }
    
    @Test
    void testGetBranchForVersioningInPullRequest() {
        System.setProperty("fcli.env.Build.Repository.Name", "ProductRepo");
        System.setProperty("fcli.env.Build.SourceBranch", "refs/pull/789/merge");
        System.setProperty("fcli.env.Build.SourceVersion", "abc123");
        System.setProperty("fcli.env.System.PullRequest.SourceBranchName", "feature-new");
        System.setProperty("fcli.env.System.PullRequest.TargetBranchName", "main");
        System.setProperty("fcli.env.System.PullRequest.PullRequestId", "789");
        
        var env = AdoEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("feature-new", env.getBranchForVersioning());
    }
    
    @Test
    void testRepositoryNameWithPath() {
        System.setProperty("fcli.env.Build.Repository.Name", "team/subteam/MyRepo");
        System.setProperty("fcli.env.Build.SourceBranch", "refs/heads/main");
        System.setProperty("fcli.env.Build.SourceVersion", "abc123");
        
        var env = AdoEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("MyRepo", env.ciRepository().name().short_());
        assertEquals("team/subteam/MyRepo", env.ciRepository().name().full());
    }
    
    @Test
    void testDefaultWorkingDirectoryFallback() {
        System.setProperty("fcli.env.Build.Repository.Name", "MyRepo");
        System.setProperty("fcli.env.Build.SourceBranch", "refs/heads/main");
        System.setProperty("fcli.env.Build.SourceVersion", "abc123");
        System.setProperty("fcli.env.System.DefaultWorkingDirectory", "/default/work");
        
        var env = AdoEnvironment.detect();
        
        assertNotNull(env);
        assertEquals("/default/work", env.ciRepository().workDir());
    }
    
    @Test
    void testFallbackToCurrentDirectory() {
        System.setProperty("fcli.env.Build.Repository.Name", "MyRepo");
        System.setProperty("fcli.env.Build.SourceBranch", "refs/heads/main");
        System.setProperty("fcli.env.Build.SourceVersion", "abc123");
        
        var env = AdoEnvironment.detect();
        
        assertNotNull(env);
        assertEquals(".", env.ciRepository().workDir());
    }
}
