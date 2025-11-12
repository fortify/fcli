/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.fod._common.scan.helper.oss;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.FoDStartScanResponse;
import com.fortify.cli.fod.oss_scan.helper.FoDScanConfigOssDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDScanOssHelper extends FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final FoDScanDescriptor startScanWithDefaults(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                                                FoDScanOssStartRequest req, File scanFile, IProgressWriter progressWriter) {
        var relId = releaseDescriptor.getReleaseId();
        HttpRequest<?> request = unirest.post(FoDUrls.OSS_SCANS_START).routeParam("relId", relId);
        return startScan(unirest, releaseDescriptor, request, scanFile, progressWriter);
    }

    private static FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, HttpRequest<?> request, File scanFile, IProgressWriter progressWriter) {
        JsonNode response = FoDFileTransferHelper.uploadChunked(unirest, request, scanFile, progressWriter);
        FoDStartScanResponse startScanResponse = JsonHelper.treeToValue(response, FoDStartScanResponse.class);
        if (startScanResponse == null || startScanResponse.getScanId() <= 0) {
            throw new FcliSimpleException("Unable to retrieve scan id from response when starting OSS scan.");
        }
        JsonNode node = objectMapper.createObjectNode()
                .put("scanId", startScanResponse.getScanId())
                .put("scanType", FoDScanType.OpenSource.name())
                .put("releaseAndScanId",  String.format("%s:%s", releaseDescriptor.getReleaseId(), startScanResponse.getScanId()))
                .put("analysisStatusType", "Pending")
                .put("applicationName", releaseDescriptor.getApplicationName())
                .put("releaseName", releaseDescriptor.getReleaseName())
                .put("microserviceName", releaseDescriptor.getMicroserviceName());
        return JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }

    public static final FoDScanConfigOssDescriptor getSetupDescriptor(UnirestInstance unirest, String relId) {
        GetRequest request = unirest.get(FoDUrls.OSS_SCANS + "/scan-setup")
                .routeParam("relId", relId);
        JsonNode setup = request.asObject(ObjectNode.class).getBody()
                .put("applicationName", "test");
        return JsonHelper.treeToValue(setup, FoDScanConfigOssDescriptor.class);
    }

    public static final FoDScanConfigOssDescriptor getSetupDescriptorWithAppRel(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        GetRequest request = unirest.get(FoDUrls.OSS_SCANS + "/scan-setup")
                .routeParam("relId", releaseDescriptor.getReleaseId());
        JsonNode setup = request.asObject(ObjectNode.class).getBody()
                .put("applicationName", releaseDescriptor.getApplicationName())
                .put("releaseName", releaseDescriptor.getReleaseName())
                .put("microserviceName", releaseDescriptor.getMicroserviceName());
        return JsonHelper.treeToValue(setup, FoDScanConfigOssDescriptor.class);
    }

    public static final JsonNode formatResults(JsonNode record) {
        String licenseSummary = "", issueSummary = "";
        JsonNode licensesNode = record.get("licenses");
        if (licensesNode.isArray()) {
            for (JsonNode license : licensesNode) {
                licenseSummary = licenseSummary.concat(license.get("name").asText()).concat(", ");
            }
        }
        if (!licenseSummary.isBlank()) licenseSummary = licenseSummary.substring(0, licenseSummary.lastIndexOf(","));
        JsonNode vulnerabilitiesNode = record.get("vulnerabilityCounts");
        
        int criticalCount = 0, highCount = 0, mediumCount = 0, lowCount = 0;
        StringBuilder issuesBuilder = new StringBuilder();

        if (vulnerabilitiesNode != null && vulnerabilitiesNode.isArray()) {
            for (JsonNode vuln : vulnerabilitiesNode) {
                String severity = vuln.path("severity").asText();
                int count = vuln.path("count").asInt(0);
                if (count > 0) {
                    issuesBuilder.append(severity).append(":").append(count).append(", ");
                }
                switch (severity) {
                    case "Critical": criticalCount += count; break;
                    case "High":     highCount += count; break;
                    case "Medium":   mediumCount += count; break;
                    case "Low":      lowCount += count; break;
                }
            }
        }
        issueSummary = issuesBuilder.length() > 0
            ? issuesBuilder.substring(0, issuesBuilder.length() - 2)
            : "";
        
        //((ObjectNode)record).remove("licenses");
        return ((ObjectNode)record)
            .put("licenseSummary", licenseSummary)
            .put("issueSummary", issueSummary)
            .put("isVulnerable", (criticalCount + highCount + mediumCount + lowCount) > 0)
            .put("openSourceCritical", criticalCount)
            .put("openSourceHigh", highCount)
            .put("openSourceMedium", mediumCount)
            .put("openSourceLow", lowCount)
            .put("issueCount", criticalCount + highCount + mediumCount + lowCount);
    }

}
