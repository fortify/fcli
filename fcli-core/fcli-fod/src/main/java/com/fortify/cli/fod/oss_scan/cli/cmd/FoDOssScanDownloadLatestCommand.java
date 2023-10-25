/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.fod.oss_scan.cli.cmd;

import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanDownloadLatestCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = FoDOutputHelperMixins.DownloadLatest.CMD_NAME)
public class FoDOssScanDownloadLatestCommand extends AbstractFoDScanDownloadLatestCommand {
    @Getter @Mixin private FoDOutputHelperMixins.DownloadLatest outputHelper;
    
    @Override
    protected GetRequest getDownloadRequest(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanDescriptor scanDescriptor) {
        return unirest.get("/api/v3/open-source-scans/{scanId}/sbom")
                .routeParam("scanId", scanDescriptor.getScanId());
    }
    
    @Override
    protected FoDScanType getScanType() {
        return FoDScanType.OpenSource;
    }

}
