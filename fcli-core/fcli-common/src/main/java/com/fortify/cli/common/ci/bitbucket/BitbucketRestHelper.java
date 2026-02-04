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
 * Bitbucket REST helper exposing Code Insights report operations. This helper
 * is shared between commands and actions to avoid duplicating request logic.
 */
@Reflectable
@RequiredArgsConstructor
public class BitbucketRestHelper {
    private final BitbucketUnirestInstanceSupplier unirestInstanceSupplier;

    public ObjectNode upsertCommitReport(String workspace, String repoSlug, String commitSha,
            String reportId, String reportContent) {
        return getUnirest()
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

    public ObjectNode addReportAnnotations(String workspace, String repoSlug, String commitSha,
            String reportId, String annotationsContent) {
        var body = annotationsContent;
        if (annotationsContent == null) {
            body = JsonHelper.getObjectMapper().createArrayNode().toString();
        }
        return getUnirest()
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

    private UnirestInstance getUnirest() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
}
