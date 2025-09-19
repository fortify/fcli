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

package com.fortify.cli.fod.dast_scan.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanStartCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDInProgressScanActionTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedHelper;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Start.CMD_NAME)
public class FoDDastAutomatedScanStartCommand extends AbstractFoDScanStartCommand {
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    @Mixin private FoDInProgressScanActionTypeMixins.DefaultOption inProgressScanActionType;
    @Option(names="--wait-interval", descriptionKey = "fcli.fod.scan.wait-interval", defaultValue = "10", required = false)
    private Integer waitInterval;
    @Option(names="--max-attempts", descriptionKey = "fcli.fod.scan.max-attempts", defaultValue = "30", required = false)
    private Integer maxAttempts;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    private String scanAction = "STARTED";

    @Override
    protected FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        String relId = releaseDescriptor.getReleaseId();

        try (var progressWriter = progressWriterFactory.create()) {

            // get current setup to ensure the scan has been configured
            FoDScanDastAutomatedHelper.getSetupDescriptor(unirest, relId);

            // check if scan is already in progress
            FoDScanDescriptor scan = FoDScanDastAutomatedHelper.handleInProgressScan(unirest, releaseDescriptor,
                    inProgressScanActionType.getInProgressScanActionType(), progressWriter, maxAttempts,
                    waitInterval);

            if (scan != null && scan.getAnalysisStatusType().equals("In_Progress")) {
                if (inProgressScanActionType.getInProgressScanActionType() == FoDEnums.InProgressScanActionType.DoNotStartScan) {
                    scanAction = "NOT_STARTED_SCAN_IN_PROGRESS";
                    return scan;
                }
            }

            return FoDScanDastAutomatedHelper.startScan(unirest, releaseDescriptor);
        }
    }

    @Override
    public final String getActionCommandResult() {
        return scanAction;
    }
}
