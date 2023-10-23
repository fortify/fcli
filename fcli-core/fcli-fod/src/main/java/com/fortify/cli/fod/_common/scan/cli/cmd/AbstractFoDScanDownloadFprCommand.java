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
package com.fortify.cli.fod._common.scan.cli.cmd;

import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

public abstract class AbstractFoDScanDownloadFprCommand extends AbstractFoDScanDownloadCommand {
    @Override
    protected GetRequest getDownloadRequest(UnirestInstance unirest, FoDScanDescriptor scanDescriptor) {
        return unirest.get("GET /api/v3/scans/{scanId}/fpr")
                .routeParam("scanId", scanDescriptor.getScanId())
                .accept("application/octet-stream")
                .queryString("scanType", scanDescriptor.getScanType());
    }
}
