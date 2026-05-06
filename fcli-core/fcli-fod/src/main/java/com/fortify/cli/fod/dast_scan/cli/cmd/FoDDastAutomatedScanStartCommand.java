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
package com.fortify.cli.fod.dast_scan.cli.cmd;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanStartCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDInProgressScanActionTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedHelper;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Start.CMD_NAME)
public class FoDDastAutomatedScanStartCommand extends AbstractFoDScanStartCommand {
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    @Mixin private FoDInProgressScanActionTypeMixins.DefaultOption inProgressScanActionType;
    @Option(names="--wait-interval", descriptionKey = "fcli.fod.scan.wait-interval", defaultValue = "10", required = false)
    private Integer waitInterval;
    @Option(names="--max-attempts", descriptionKey = "fcli.fod.scan.max-attempts", defaultValue = "30", required = false)
    private Integer maxAttempts;
    @Option(names = {"--vpn"})
    private String fodConnectNetwork;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    private String scanAction = "STARTED";

    @Override
    protected FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        String relId = releaseDescriptor.getReleaseId();

        try (var progressWriter = progressWriterFactory.create()) {

            // get current setup to ensure the scan has been configured
            var setup = FoDScanDastAutomatedHelper.getSetupDescriptor(unirest, relId);
            if (setup == null) {
                throw new FcliSimpleException("DAST Automated scan is not configured for release '" + releaseDescriptor.getReleaseName() + "'. Please run one of the 'fod dast-scan setup-xxx' commands to configure the scan before starting.");
            }

            // handle any in-progress or active scans according to the configured action
            FoDScanDescriptor scan = FoDScanDastAutomatedHelper.handleInProgressScan(unirest, releaseDescriptor,
                    inProgressScanActionType.getInProgressScanActionType(), progressWriter, maxAttempts,
                    waitInterval);

            // if the action was to not start a new scan, return the in progress scan descriptor
            if (scan != null && scan.getAnalysisStatusType().equals("In_Progress")) {
                if (inProgressScanActionType.getInProgressScanActionType() == FoDEnums.InProgressScanActionType.DoNotStartScan) {
                    scanAction = "NOT_STARTED_SCAN_IN_PROGRESS";
                    return scan;
                }
            }

            try {
                return FoDScanDastAutomatedHelper.startScan(unirest, fodConnectNetwork, releaseDescriptor);
            } catch (UnexpectedHttpResponseException e) {
                // If FoD rejects start because a dynamic scan is in progress, try to fetch that scan and return it
                if (e.getStatus() == 422 && (e.getMessage().contains("dynamic scan is currently in progress") || e.getMessage().contains("errorCode: -10"))) {
                    FoDScanDescriptor running = FoDScanDastAutomatedHelper.handleInProgressScan(unirest, releaseDescriptor,
                            FoDEnums.InProgressScanActionType.DoNotStartScan, progressWriter, maxAttempts,
                            waitInterval);
                    if (running != null) {
                        scanAction = "NOT_STARTED_SCAN_IN_PROGRESS";
                        return running;
                    }
                }
                throw e;
            }
        }
    }

    @Override
    public final String getActionCommandResult() {
        return scanAction;
    }

}
