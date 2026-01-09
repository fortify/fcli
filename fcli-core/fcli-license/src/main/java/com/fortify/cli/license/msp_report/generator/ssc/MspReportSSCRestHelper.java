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
package com.fortify.cli.license.msp_report.generator.ssc;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.ssc._common.rest.ssc.helper.SSCInputTransformer;
import com.fortify.cli.ssc._common.rest.ssc.helper.SSCPagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * SSC REST API helper providing core operations for MSP report generation.
 * This class encapsulates SSC-specific API interactions for loading projects,
 * application versions, and artifacts.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class MspReportSSCRestHelper {
    private final MspReportSSCUnirestInstanceSupplier unirestInstanceSupplier;
    
    /**
     * Process all SSC projects (applications) with paging support.
     * 
     * @param processor Consumer to process each page of projects
     */
    public void processProjectPages(Consumer<HttpResponse<JsonNode>> processor) {
        SSCPagingHelper.pagedRequest(getUnirest().get("/api/v1/projects?limit=100"))
            .forEach(processor);
    }
    
    /**
     * Process all application versions for a specific project.
     * 
     * @param projectId Project ID
     * @param processor Consumer to process each page of versions
     */
    public void processProjectVersionPages(String projectId, Consumer<HttpResponse<JsonNode>> processor) {
        SSCPagingHelper.pagedRequest(
            getUnirest().get("/api/v1/projects/{id}/versions?limit=100")
                .routeParam("id", projectId))
            .forEach(processor);
    }
    
    /**
     * Process all artifacts for a specific application version.
     * 
     * @param versionId Application version ID
     * @param processor Consumer to process each page of artifacts
     */
    public void processArtifactPages(String versionId, Consumer<HttpResponse<JsonNode>> processor) {
        HttpRequest<?> req = getUnirest().get("/api/v1/projectVersions/{pvId}/artifacts?limit=100&embed=scans")
            .routeParam("pvId", versionId);
        SSCPagingHelper.pagedRequest(req)
            .forEach(processor);
    }
    
    /**
     * Process artifacts with custom paging control for early termination.
     * 
     * @param versionId Application version ID
     * @param continueNextPageSupplier Supplier to control whether to continue to next page
     * @param processor Consumer to process each page of artifacts
     */
    public void processArtifactPages(String versionId, SSCPagingHelper.SSCContinueNextPageSupplier continueNextPageSupplier, Consumer<HttpResponse<JsonNode>> processor) {
        HttpRequest<?> req = getUnirest().get("/api/v1/projectVersions/{pvId}/artifacts?limit=100&embed=scans")
            .routeParam("pvId", versionId);
        SSCPagingHelper.pagedRequest(req, continueNextPageSupplier)
            .forEach(processor);
    }
    
    /**
     * Extract data array from SSC response body.
     * 
     * @param body Response body containing data
     * @return ArrayNode containing the data items
     */
    public ArrayNode extractData(JsonNode body) {
        return (ArrayNode) SSCInputTransformer.getDataOrSelf(body);
    }
    
    /**
     * Get the UnirestInstance from the supplier.
     */
    public UnirestInstance getUnirest() {
        return unirestInstanceSupplier.getUnirestInstance();
    }
}
