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
package com.fortify.cli.common.rest.ci.gitlab;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Generic GitLab REST API helper providing core operations for projects,
 * merge requests, security reports, and other GitLab features. This class can be
 * used from commands, actions, and other modules like fcli-license.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class GitLabRestHelper {
    private final GitLabUnirestInstanceSupplier unirestInstanceSupplier;
    
    // === Security Report Upload ===
    
    /**
     * Upload security report to GitLab (SAST, DAST, dependency scanning, etc.).
     * 
     * @param projectId Project ID
     * @param pipelineId Pipeline ID
     * @param reportType Report type (sast, dast, dependency_scanning, etc.)
     * @param reportContent Report content (JSON format)
     * @return Response from GitLab API
     */
    public ObjectNode uploadSecurityReport(int projectId, int pipelineId, 
                                            String reportType, String reportContent) {
        return getUnirest()
            .post("/projects/{id}/pipelines/{pipeline_id}/security_report_summary")
            .routeParam("id", String.valueOf(projectId))
            .routeParam("pipeline_id", String.valueOf(pipelineId))
            .queryString("report_type", reportType)
            .header("Content-Type", "application/json")
            .body(reportContent)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Merge Request Operations ===
    
    /**
     * Create a note (comment) on a merge request.
     * 
     * @param projectId Project ID
     * @param mergeRequestIid Merge request IID (internal ID)
     * @param body Comment body (Markdown supported)
     * @return Created note object
     */
    public ObjectNode createMergeRequestNote(int projectId, int mergeRequestIid, String body) {
        return getUnirest()
            .post("/projects/{id}/merge_requests/{merge_request_iid}/notes")
            .routeParam("id", String.valueOf(projectId))
            .routeParam("merge_request_iid", String.valueOf(mergeRequestIid))
            .body(JsonHelper.getObjectMapper().createObjectNode().put("body", body))
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Project Operations ===
    
    /**
     * Process all projects in a group.
     * 
     * @param groupId Group ID
     * @param includeSubgroups Whether to include subgroup projects
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processGroupProjects(String groupId, boolean includeSubgroups, Function<JsonNode, Break> processor) {
        new GitLabGroupProjectsProcessor(getUnirest(), groupId, includeSubgroups).process(processor);
    }

    /**
     * Process branches for a project.
     * 
     * @param projectId Project ID
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processBranches(String projectId, Function<JsonNode, Break> processor) {
        new GitLabBranchesProcessor(getUnirest(), projectId).process(processor);
    }

    /**
     * Process commits for a project/branch.
     * 
     * @param projectId Project ID
     * @param refName Branch name or commit SHA
     * @param since ISO 8601 timestamp to filter commits after this date
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processCommits(String projectId, String refName, String since, Function<JsonNode, Break> processor) {
        new GitLabCommitsProcessor(getUnirest(), projectId, refName, since).process(processor);
    }
    
    /**
     * Get the latest commit for a specific branch.
     * 
     * @param projectId Project ID (String or int)
     * @param branchName Branch name
     * @return ArrayNode containing the commit (single element)
     */
    public ArrayNode getLatestCommit(String projectId, String branchName) {
        return getUnirest().get("/projects/{projectId}/repository/commits?ref_name={branchName}")
            .routeParam("projectId", projectId)
            .routeParam("branchName", branchName)
            .queryString("per_page", 1)
            .asObject(ArrayNode.class)
            .getBody();
    }
    

    
    // === Internal Methods ===
    
    /**
     * Get the UnirestInstance from the supplier.
     */
    private UnirestInstance getUnirest() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
    
    // === Inner Classes ===
    
    @RequiredArgsConstructor
    private static final class GitLabGroupProjectsProcessor {
        private final UnirestInstance unirest;
        private final String groupId;
        private final boolean includeSubgroups;
        
        public void process(Function<JsonNode, Break> processor) {
            GitLabPagingHelper.processPagedItems(
                unirest,
                unirest.get("/groups/{id}/projects")
                    .routeParam("id", groupId)
                    .queryString("include_subgroups", includeSubgroups),
                processor
            );
        }
    }
    
    @RequiredArgsConstructor
    private static final class GitLabBranchesProcessor {
        private final UnirestInstance unirest;
        private final String projectId;
        
        public void process(Function<JsonNode, Break> processor) {
            GitLabPagingHelper.processPagedItems(
                unirest,
                unirest.get("/projects/{id}/repository/branches")
                    .routeParam("id", projectId),
                processor
            );
        }
    }
    
    @RequiredArgsConstructor
    private static final class GitLabCommitsProcessor {
        private final UnirestInstance unirest;
        private final String projectId;
        private final String refName;
        private final String since;
        
        public void process(Function<JsonNode, Break> processor) {
            var request = unirest.get("/projects/{id}/repository/commits")
                .routeParam("id", projectId)
                .queryString("ref_name", refName);
            if (since != null) {
                request = request.queryString("since", since);
            }
            GitLabPagingHelper.processPagedItems(unirest, request, processor);
        }
    }
}
