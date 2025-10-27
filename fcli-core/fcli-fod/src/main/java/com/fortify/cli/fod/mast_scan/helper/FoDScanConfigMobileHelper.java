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
package com.fortify.cli.fod.mast_scan.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

public class FoDScanConfigMobileHelper {
    public static final FoDScanConfigMobileDescriptor getSetupDescriptor(UnirestInstance unirest, String relId) {
        var body = unirest.get(FoDUrls.MOBILE_SCANS + "/scan-setup")
                .routeParam("relId", relId)
                .asObject(ObjectNode.class)
                .getBody();
        return JsonHelper.treeToValue(body, FoDScanConfigMobileDescriptor.class);
    }

    public static final FoDScanConfigMobileDescriptor setupScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanConfigMobileSetupRequest setupMobileScanRequest) {
        var releaseId = releaseDescriptor.getReleaseId();
        unirest.put(FoDUrls.MOBILE_SCANS + "/scan-setup")
                .routeParam("relId", releaseId)
                .body(setupMobileScanRequest)
                .asString().getBody();
        return getSetupDescriptor(unirest, releaseId);
    }

    public static final FoDScanConfigMobileDescriptor getSetupDescriptorWithAppRel(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        GetRequest request = unirest.get(FoDUrls.MOBILE_SCANS + "/scan-setup")
                .routeParam("relId", releaseDescriptor.getReleaseId());
        JsonNode setup = request.asObject(ObjectNode.class).getBody()
                .put("applicationName", releaseDescriptor.getApplicationName())
                .put("releaseName", releaseDescriptor.getReleaseName())
                .put("microserviceName", releaseDescriptor.getMicroserviceName());
        return JsonHelper.treeToValue(setup, FoDScanConfigMobileDescriptor.class);
    }

}
