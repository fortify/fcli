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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Repository-scoped Bitbucket REST API operations. This class provides methods
 * for interacting with a specific Bitbucket repository including Code Insights reports.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class BitbucketRepository {
    private final UnirestInstance unirest;
    private final String workspace;
    private final String repoSlug;
    
    /**
     * Create or update a Code Insights report for a specific commit.
     * 
     * @param commitSha Commit SHA
     * @param reportId Report identifier
     * @param reportContent Report content in Bitbucket Code Insights format
     * @return Response from Bitbucket API
     */
    public ObjectNode upsertCommitReport(String commitSha, String reportId, String reportContent) {
        return unirest
            .put("/repositories/{workspace}/{repo_slug}/commit/{commit}/reports/{report_id}")
            .routeParam("workspace", workspace)
            .routeParam("repo_slug", repoSlug)
            .routeParam("commit", commitSha)
            .routeParam("report_id", reportId)
            .header("Content-Type", "application/json")
            .body(reportContent)
            .asObject(ObjectNode.class)
            .getBody();
    }
    
    /**
     * Add annotations to an existing Code Insights report.
     * 
     * @param commitSha Commit SHA
     * @param reportId Report identifier
     * @param annotationsContent Annotations content (JSON array)
     * @return Response from Bitbucket API
     */
    public ObjectNode addReportAnnotations(String commitSha, String reportId, String annotationsContent) {
        var body = annotationsContent;
        if (annotationsContent == null) {
            body = JsonHelper.getObjectMapper().createArrayNode().toString();
        }
        return unirest
            .post("/repositories/{workspace}/{repo_slug}/commit/{commit}/reports/{report_id}/annotations")
            .routeParam("workspace", workspace)
            .routeParam("repo_slug", repoSlug)
            .routeParam("commit", commitSha)
            .routeParam("report_id", reportId)
            .header("Content-Type", "application/json")
            .body(body)
            .asObject(ObjectNode.class)
            .getBody();
    }
}
