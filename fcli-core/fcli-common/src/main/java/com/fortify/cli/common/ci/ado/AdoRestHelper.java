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

import com.formkiq.graalvm.annotations.Reflectable;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Azure DevOps REST API helper factory providing fluent API for repository, project, 
 * and organization operations. This class can be used from commands, actions, and 
 * other modules like fcli-license.
 * 
 * <p>Usage examples:
 * <pre>{@code
 * // Repository-scoped operations
 * var repo = restHelper.repository("org", "project", "repo-id");
 * repo.uploadSarif(ref, commitSha, sarifContent);
 * repo.queryBranches().process(branch -> ...);
 * repo.queryCommits().branchName("main").fromDate("2024-01-01").process(commit -> ...);
 * 
 * // Project-scoped operations
 * var project = restHelper.project("org", "project");
 * project.publishTestResults(buildId, testResults, testRunner);
 * project.queryRepositories().process(repo -> ...);
 * 
 * // Organization-scoped operations
 * var org = restHelper.organization("organization");
 * org.queryProjects().process(project -> ...);
 * }</pre>
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class AdoRestHelper {
    private final AdoUnirestInstanceSupplier unirestInstanceSupplier;
    
    /**
     * Create a repository-scoped API client for the specified repository.
     * 
     * @param organization Organization name
     * @param project Project name or ID
     * @param repositoryId Repository ID
     * @return Repository-scoped client
     */
    public AdoRepository repository(String organization, String project, String repositoryId) {
        return new AdoRepository(getUnirest(), organization, project, repositoryId);
    }
    
    /**
     * Create a project-scoped API client for the specified project.
     * 
     * @param organization Organization name
     * @param project Project name or ID
     * @return Project-scoped client
     */
    public AdoProject project(String organization, String project) {
        return new AdoProject(getUnirest(), organization, project);
    }
    
    /**
     * Create an organization-scoped API client for the specified organization.
     * 
     * @param organization Organization name
     * @return Organization-scoped client
     */
    public AdoOrganization organization(String organization) {
        return new AdoOrganization(getUnirest(), organization);
    }
    
    /**
     * Get the UnirestInstance from the supplier.
     */
    private UnirestInstance getUnirest() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
}
