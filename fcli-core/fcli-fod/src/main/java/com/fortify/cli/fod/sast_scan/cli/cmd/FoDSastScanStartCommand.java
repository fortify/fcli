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
package com.fortify.cli.fod.sast_scan.cli.cmd;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanStartCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDRemediationScanPreferenceTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.sast.FoDScanSastHelper;
import com.fortify.cli.fod._common.scan.helper.sast.FoDScanSastStartRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.sast_scan.helper.FoDScanConfigSastDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Start.CMD_NAME, hidden = false)
public class FoDSastScanStartCommand extends AbstractFoDScanStartCommand {
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    @Option(names = {"--notes"})
    private String notes;
    @Option(names = {"--in-progress-action"}, descriptionKey = "fcli.fod.sast-scan.start.in-progress-action")
    private FoDEnums.InProgressScanActionType inProgressScanActionType;
    @Option(names = {"--entitlement-preference"}, descriptionKey = "fcli.fod.scan.entitlement-preference")
    private FoDEnums.EntitlementPreferenceType entitlementPreferenceType;
    @Mixin private CommonOptionMixins.RequiredFile scanFileMixin;

    @Mixin private FoDRemediationScanPreferenceTypeMixins.OptionalOption remediationScanType;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    
    @Override
    protected FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        String relId = releaseDescriptor.getReleaseId();

        validateScanSetup(unirest, relId);

        FoDEnums.RemediationScanPreferenceType remediationPref = remediationScanType != null
                ? remediationScanType.getRemediationScanPreferenceType() : null;

        boolean useAdvanced = entitlementPreferenceType != null || inProgressScanActionType != null;

        FoDScanSastStartRequest.FoDScanSastStartRequestBuilder requestBuilder = FoDScanSastStartRequest.builder()
                .scanMethodType("Other")
                .notes(notes != null && !notes.isEmpty() ? notes : "")
                .scanTool(FcliBuildProperties.INSTANCE.getFcliProjectName())
                .scanToolVersion(FcliBuildProperties.INSTANCE.getFcliVersion());

        try (IProgressWriter progressWriter = progressWriterFactory.create()) {
            if (useAdvanced) {
                FoDEnums.InProgressScanActionType inProgressAction = inProgressScanActionType != null
                        ? inProgressScanActionType : FoDEnums.InProgressScanActionType.Queue;
                // FoD's start-scan-advanced expects 'CancelInProgressScan' rather than the enum's 'CancelScanInProgress'
                String inProgressApiValue = inProgressAction == FoDEnums.InProgressScanActionType.CancelScanInProgress
                        ? "CancelInProgressScan" : inProgressAction.name();
                FoDScanSastStartRequest startScanRequest = requestBuilder
                        .entitlementPreferenceType(entitlementPreferenceType != null ? entitlementPreferenceType.name() : null)
                        .purchaseEntitlement(false)
                        .remdiationScanPreferenceType(remediationPref != null ? remediationPref.name() : null)
                        .inProgressScanActionType(inProgressApiValue)
                        .build();
                return FoDScanSastHelper.startScanAdvanced(unirest, releaseDescriptor, startScanRequest, scanFileMixin.getFile(), progressWriter);
            }
            boolean isRemediation = remediationPref != null
                    && (remediationPref.equals(FoDEnums.RemediationScanPreferenceType.RemediationScanIfAvailable)
                            || remediationPref.equals(FoDEnums.RemediationScanPreferenceType.RemediationScanOnly));
            FoDScanSastStartRequest startScanRequest = requestBuilder
                    .isRemediationScan(isRemediation)
                    .build();
            return FoDScanSastHelper.startScanWithDefaults(unirest, releaseDescriptor, startScanRequest, scanFileMixin.getFile(), progressWriter);
        } catch (Exception e) {
            throw translateScanInProgressException(e);
        }
    }

    // FoD returns HTTP 422 (errorCode 2001) when a scan is already in progress and the
    // in-progress action prevents starting a new one. Translate that into a concise,
    // actionable message instead of surfacing the raw upload/HTTP exception.
    private RuntimeException translateScanInProgressException(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof UnexpectedHttpResponseException) {
                UnexpectedHttpResponseException httpException = (UnexpectedHttpResponseException) t;
                if (httpException.getStatus() == 422 && httpException.getMessage() != null
                        && httpException.getMessage().toLowerCase().contains("another scan is in progress")) {
                    return new FcliSimpleException("Cannot start scan: another scan is already in progress for this release. "
                            + "Use '--in-progress-action=Queue' to queue this scan, or "
                            + "'--in-progress-action=CancelScanInProgress' to cancel the running scan and start a new one.");
                }
            }
        }
        return e instanceof RuntimeException ? (RuntimeException) e : new FcliSimpleException(e);
    }

    private void validateScanSetup(UnirestInstance unirest, String relId) {
        // get current setup and check if its valid
        FoDScanConfigSastDescriptor currentSetup = FoDScanSastHelper.getSetupDescriptor(unirest, relId);
        if (validateEntitlement) {
            if (currentSetup.getEntitlementId() == null || currentSetup.getEntitlementId() <= 0) {
                throw new FcliSimpleException("The static scan configuration for release with id '" + relId +
                        "' has not been setup correctly - 'Entitlement' is missing or empty.");
            }
        }
        if (StringUtils.isBlank(currentSetup.getTechnologyStack())) {
            throw new FcliSimpleException("The static scan configuration for release with id '" + relId +
                    "' has not been setup correctly - 'Technology Stack/Language Level' is missing or empty.");
        }
    }

}
