/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod._common.scan.helper.oss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
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

import java.io.File;

public class FoDScanOssHelper extends FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final FoDScanDescriptor startScanWithDefaults(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                                                FoDScanOssStartRequest req, File scanFile) {
        var relId = releaseDescriptor.getReleaseId();
        HttpRequest<?> request = unirest.post(FoDUrls.OSS_SCANS_START).routeParam("relId", relId);
        return startScan(unirest, releaseDescriptor, request, scanFile);
    }

    private static FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, HttpRequest<?> request, File scanFile) {
        JsonNode response = FoDFileTransferHelper.uploadChunked(unirest, request, scanFile);
        FoDStartScanResponse startScanResponse = JsonHelper.treeToValue(response, FoDStartScanResponse.class);
        if (startScanResponse == null || startScanResponse.getScanId() <= 0) {
            throw new RuntimeException("Unable to retrieve scan id from response when starting OSS scan.");
        }
        JsonNode node = objectMapper.createObjectNode()
                .put("scanId", startScanResponse.getScanId())
                .put("scanType", FoDScanType.OpenSource.name())
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

}
