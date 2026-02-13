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

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.Break;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Generic Azure DevOps REST API helper providing core operations for builds,
 * pull requests, code analysis results, and other Azure DevOps features. This class can be
 * used from commands, actions, and other modules like fcli-license.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class AdoRestHelper {
    private final AdoUnirestInstanceSupplier unirestInstanceSupplier;
    
    // === SARIF Upload (Advanced Security) ===
    
    /**
     * Upload SARIF report to Azure DevOps Advanced Security.
     * Requires GitHub Advanced Security for Azure DevOps license.
     * 
     * SARIF format: https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html
     * ADO Advanced Security: https://learn.microsoft.com/en-us/azure/devops/repos/security/github-advanced-security
     * 
     * @param organization Organization name
     * @param project Project name or ID
     * @param repositoryId Repository ID
     * @param ref Git ref (branch/tag)
     * @param commitSha Commit SHA
     * @param sarifContent SARIF report content
     * @return Response from Azure DevOps API
     */
    public ObjectNode uploadSarif(String organization, String project,
                                   String repositoryId, String ref, String commitSha,
                                   String sarifContent) {
        var body = JsonHelper.getObjectMapper().createObjectNode()
            .put("repository", repositoryId)
            .put("ref", ref)
            .put("commitSha", commitSha)
            .put("sarif", sarifContent);
        
        return getUnirest()
            .post("/{organization}/{project}/_apis/alert/sarif")
            .routeParam("organization", organization)
            .routeParam("project", project)
            .queryString("api-version", "7.1-preview.1")
            .header("Content-Type", "application/json")
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Publish test results to Azure DevOps (available on all tiers).
     * While primarily for test results, this can be adapted for security findings on free tier.
     * 
     * Supported formats: JUnit, NUnit, XUnit, VSTest, CTest
     * API documentation: https://learn.microsoft.com/en-us/rest/api/azure/devops/test/results
     * 
     * For security findings, format as test failures where:
     * - Test name = vulnerability title
     * - Error message = vulnerability description  
     * - Stack trace = file path and line number
     * 
     * @param project Project name or ID
     * @param buildId Build ID
     * @param testResults Test results in specified format
     * @param testRunner Test runner type (JUnit, NUnit, XUnit, VSTest, CTest)
     * @return Response from Azure DevOps API
     */
    public ObjectNode publishTestResults(String project, String buildId,
                                          String testResults, String testRunner) {
        var body = JsonHelper.getObjectMapper().createObjectNode()
            .put("testRunner", testRunner)
            .put("results", testResults);
        
        return getUnirest()
            .post("/{project}/_apis/test/runs")
            .routeParam("project", project)
            .queryString("api-version", "7.0")
            .header("Content-Type", "application/json")
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Pull Request Operations ===
    
    /**
     * Create a comment thread on a pull request.
     * 
     * @param project Project name or ID
     * @param repositoryId Repository ID
     * @param pullRequestId Pull request ID
     * @param comment Comment text
     * @return Created thread object
     */
    public ObjectNode createPullRequestThread(String project, String repositoryId,
                                               String pullRequestId, String comment) {
        var body = JsonHelper.getObjectMapper().createObjectNode();
        var commentsArray = body.putArray("comments");
        commentsArray.addObject().put("content", comment);
        
        return getUnirest()
            .post("/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}/threads")
            .routeParam("project", project)
            .routeParam("repositoryId", repositoryId)
            .routeParam("pullRequestId", pullRequestId)
            .queryString("api-version", "7.0")
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Project Enumeration ===
    
    /**
     * Process all projects in the organization.
     * 
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processProjects(Function<JsonNode, Break> processor) {
        var request = getUnirest()
            .get("/_apis/projects")
            .queryString("api-version", "7.0");
        
        AdoPagingHelper.processPagedItems(getUnirest(), request, processor);
    }
    
    // === Repository Enumeration ===
    
    /**
     * Process repositories for a project.
     * Note: ADO doesn't support organization-level repository enumeration,
     * only project-level.
     * 
     * @param project Project name or ID
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processRepositories(String project, Function<JsonNode, Break> processor) {
        var request = getUnirest()
            .get("/{project}/_apis/git/repositories")
            .routeParam("project", project)
            .queryString("api-version", "7.0");
        
        AdoPagingHelper.processPagedItems(getUnirest(), request, processor);
    }
    
    /**
     * Process branches for a repository.
     * 
     * @param project Project name or ID
     * @param repositoryId Repository ID
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processBranches(String project, String repositoryId, Function<JsonNode, Break> processor) {
        var request = getUnirest()
            .get("/{project}/_apis/git/repositories/{repositoryId}/refs")
            .routeParam("project", project)
            .routeParam("repositoryId", repositoryId)
            .queryString("filter", "heads/")
            .queryString("api-version", "7.0");
        
        AdoPagingHelper.processPagedItems(getUnirest(), request, processor);
    }
    
    /**
     * Process commits for a repository.
     * 
     * @param project Project name or ID
     * @param repositoryId Repository ID
     * @param branchName Branch name
     * @param fromDate ISO 8601 timestamp to filter commits after this date (optional)
     * @param processor Function that returns Break.TRUE to stop processing, Break.FALSE to continue
     */
    public void processCommits(String project, String repositoryId, String branchName, String fromDate, 
                                Function<JsonNode, Break> processor) {
        var request = getUnirest()
            .get("/{project}/_apis/git/repositories/{repositoryId}/commits")
            .routeParam("project", project)
            .routeParam("repositoryId", repositoryId)
            .queryString("searchCriteria.itemVersion.version", branchName)
            .queryString("searchCriteria.itemVersion.versionType", "branch")
            .queryString("searchCriteria.$top", "100")
            .queryString("api-version", "7.0");
        
        if (fromDate != null) {
            request = request.queryString("searchCriteria.fromDate", fromDate);
        }
        
        AdoPagingHelper.processPagedItems(getUnirest(), request, processor);
    }
    
    /**
     * Get the latest commit for a specific branch.
     * 
     * @param project Project name or ID
     * @param repositoryId Repository ID
     * @param branchName Branch name
     * @return ObjectNode containing commit data
     */
    public ObjectNode getLatestCommit(String project, String repositoryId, String branchName) {
        return getUnirest()
            .get("/{project}/_apis/git/repositories/{repositoryId}/commits")
            .routeParam("project", project)
            .routeParam("repositoryId", repositoryId)
            .queryString("searchCriteria.itemVersion.version", branchName)
            .queryString("searchCriteria.itemVersion.versionType", "branch")
            .queryString("searchCriteria.$top", "1")
            .queryString("api-version", "7.0")
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    // === Internal Methods ===
    
    /**
     * Get the UnirestInstance from the supplier.
     */
    private UnirestInstance getUnirest() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
}
