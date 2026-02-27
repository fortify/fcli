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

import com.formkiq.graalvm.annotations.Reflectable;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * GitLab REST API helper factory providing fluent API for project and group operations.
 * This class can be used from commands, actions, and other modules like fcli-license.
 * 
 * <p>Usage examples:
 * <pre>{@code
 * // Project-scoped operations
 * var project = restHelper.project("12345");
 * project.uploadSecurityReport(pipelineId, reportType, reportContent);
 * project.queryBranches().process(branch -> ...);
 * project.queryCommits().refName("main").since("2024-01-01").process(commit -> ...);
 * 
 * // Group-scoped operations
 * var group = restHelper.group("group-id");
 * group.queryProjects().includeSubgroups(true).process(project -> ...);
 * }</pre>
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class GitLabRestHelper {
    private final GitLabUnirestInstanceSupplier unirestInstanceSupplier;
    
    /**
     * Create a project-scoped API client for the specified project.
     * 
     * @param projectId Project ID
     * @return Project-scoped client
     */
    public GitLabProject project(String projectId) {
        return new GitLabProject(getUnirest(), projectId);
    }
    
    /**
     * Create a group-scoped API client for the specified group.
     * 
     * @param groupId Group ID
     * @return Group-scoped client
     */
    public GitLabGroup group(String groupId) {
        return new GitLabGroup(getUnirest(), groupId);
    }
    
    /**
     * Get the UnirestInstance from the supplier.
     */
    private UnirestInstance getUnirest() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
}
