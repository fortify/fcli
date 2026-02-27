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

import com.formkiq.graalvm.annotations.Reflectable;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * GitHub REST API helper factory providing fluent API for repository and organization operations.
 * This class can be used from commands, actions, and other modules like fcli-license.
 * 
 * <p>Usage examples:
 * <pre>{@code
 * // Repository-scoped operations
 * var repo = restHelper.repo("owner", "repo-name");
 * repo.uploadSarif(ref, sarifContent, commitSha);
 * repo.queryBranches().process(branch -> ...);
 * repo.queryCommits().sha("main").since("2024-01-01").process(commit -> ...);
 * 
 * // Organization-scoped operations
 * var org = restHelper.org("owner");
 * org.queryRepositories().process(repo -> ...);
 * }</pre>
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class GitHubRestHelper {
    private final GitHubUnirestInstanceSupplier unirestInstanceSupplier;
    
    /**
     * Create a repository-scoped API client for the specified repository.
     * 
     * @param owner Repository owner (organization or user)
     * @param repo Repository name
     * @return Repository-scoped client
     */
    public GitHubRepo repo(String owner, String repo) {
        return new GitHubRepo(getUnirest(), owner, repo);
    }
    
    /**
     * Create an organization-scoped API client for the specified organization/user.
     * 
     * @param owner Organization or user name
     * @return Organization-scoped client
     */
    public GitHubOrg org(String owner) {
        return new GitHubOrg(getUnirest(), owner);
    }
    
    /**
     * Get the UnirestInstance from the supplier.
     */
    private UnirestInstance getUnirest() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
}
