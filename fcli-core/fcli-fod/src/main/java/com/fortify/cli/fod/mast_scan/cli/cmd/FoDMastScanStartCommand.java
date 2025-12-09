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
package com.fortify.cli.fod.mast_scan.cli.cmd;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanStartCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDRemediationScanPreferenceTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.mobile.FoDScanMobileHelper;
import com.fortify.cli.fod._common.scan.helper.mobile.FoDScanMobileStartRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileDescriptor;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Start.CMD_NAME)
public class FoDMastScanStartCommand extends AbstractFoDScanStartCommand {
    private static final Logger LOG = LoggerFactory.getLogger(FoDMastScanStartCommand.class);
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    @Option(names = {"--assessment-type"}, required = true)
    private String mobileAssessmentType;
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    private enum MobileFrameworks { iOS, Android }
    @Option(names = {"--framework"})
    private MobileFrameworks mobileFramework;
    private enum MobilePlatforms { Phone, Tablet, Both }
    @Option(names = {"--platform"})
    private MobilePlatforms mobilePlatform;
    @Option(names = {"--timezone"})
    private String timezone;
    @Option(names = {"--start-date"})
    private String startDate;
    @Option(names = {"--notes"})
    private String notes;
    @Mixin private CommonOptionMixins.RequiredFile scanFileMixin;
    @Mixin private FoDEntitlementFrequencyTypeMixins.RequiredOption entitlementFrequencyTypeMixin;
    @Mixin private FoDRemediationScanPreferenceTypeMixins.OptionalOption remediationScanType;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    @Override
    protected FoDScanDescriptor startScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        String relId = releaseDescriptor.getReleaseId();
        Boolean isRemediation = false;

        // validate if the release has been setup for scanning
        FoDScanConfigMobileDescriptor currentSetup = validateScanSetup(unirest, relId);

        // if we have requested remediation scan use it to find appropriate assessment type
        if (remediationScanType != null && remediationScanType.getRemediationScanPreferenceType() != null) {
            if (remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.RemediationScanIfAvailable) ||
                    remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.RemediationScanOnly)) {
                isRemediation = true;
            }
        }

        LOG.info("Finding appropriate entitlement to use.");
        var atd = FoDReleaseAssessmentTypeHelper.getAssessmentTypeDescriptor(unirest, relId, FoDScanType.Mobile, 
        entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), mobileAssessmentType);
        var assessmentTypeId = atd.getAssessmentTypeId();
        var entitlementIdToUse = atd.getEntitlementId();
        validateEntitlement(currentSetup, entitlementIdToUse, relId, atd);
        LOG.info("Configuring release to use entitlement " + entitlementIdToUse);

        // validate timezone (if specified)
        String timeZoneToUse = FoDScanHelper.validateTimezone(unirest, timezone);

        // if start date is not specified use the current date/time
        String startDateStr = (startDate == null || startDate.isEmpty())
                ? LocalDateTime.now().format(dtf)
                : LocalDateTime.parse(startDate, dtf).toString();

        // if mobileFramework is not specified use the one from the current setup
        String frameworkType = (mobileFramework == null) ? 
                currentSetup.getFrameworkType() 
                : mobileFramework.name();

        // if mobilePlatform is not specified then set to "Phone" as default
        // Note: we can't currently retrieve this from current setup using the API
        String platformType = (mobilePlatform == null) ? 
                MobilePlatforms.Phone.name() 
                : mobilePlatform.name();

        FoDScanMobileStartRequest startScanRequest = FoDScanMobileStartRequest.builder()
                .startDate(startDateStr)
                .assessmentTypeId(assessmentTypeId)
                .entitlementId(entitlementIdToUse)
                .entitlementFrequencyType(entitlementFrequencyTypeMixin.getEntitlementFrequencyType().name())
                .isRemediationScan(isRemediation)
                .timeZone(timeZoneToUse)
                .frameworkType(frameworkType)
                .platformType(platformType)
                .scanMethodType("Other")
                .notes(notes != null && !notes.isEmpty() ? notes : "")
                .scanTool(FcliBuildProperties.INSTANCE.getFcliProjectName())
                .scanToolVersion(FcliBuildProperties.INSTANCE.getFcliVersion()).build();

        try (IProgressWriter progressWriter = progressWriterFactory.create()) {
            return FoDScanMobileHelper.startScan(unirest, releaseDescriptor, startScanRequest, scanFileMixin.getFile(), progressWriter);
        }
    }

    private void validateEntitlement(FoDScanConfigMobileDescriptor currentSetup, Integer entitlementIdToUse, String relId, FoDReleaseAssessmentTypeDescriptor atd) {
        // validate entitlement specified or currently in use against assessment type found
        if (entitlementId != null && entitlementId > 0) {
            // check if "entitlement id" explicitly matches what has been found
            if (!Objects.equals(entitlementIdToUse, entitlementId)) {
                throw new FcliSimpleException("Cannot appropriate assessment type for use with entitlement: " + entitlementId);
            }
        } else {
            if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
                // check if "entitlement id" is already configured
                if (!Objects.equals(entitlementIdToUse, currentSetup.getEntitlementId())) {
                    LOG.warn("Changing current release entitlement from " + currentSetup.getEntitlementId());
                }
            }
        }

        // check if the entitlement can still be used
        if (FoDReleaseAssessmentTypeHelper.validateEntitlementCanBeUsed(relId, atd)) {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is still valid.");
        } else {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is no longer valid.");
        }
    }


    private FoDScanConfigMobileDescriptor validateScanSetup(UnirestInstance unirest, String relId) {
        FoDScanConfigMobileDescriptor currentSetup = FoDScanConfigMobileHelper.getSetupDescriptor(unirest, relId);
        // we cannot use the below yet as "/scan-setup" does not currently allow setting entitlement
        //if (validateEntitlement) {
        //    if (currentSetup.getEntitlementId() == null || currentSetup.getEntitlementId() <= 0) {
        //        throw new FcliSimpleException("The mobile scan configuration for release with id '" + relId +
        //                "' has not been setup correctly - 'Entitlement' is missing or empty.");
        //    }
        //}
        if (StringUtils.isBlank(currentSetup.getAuditPreferenceType())) {
            throw new FcliSimpleException("The static scan configuration for release with id '" + relId +
                    "' has not been setup correctly - 'Audit Preference' is missing or empty. Please use the `mast-scan setup` command to configure.");
        }
        return currentSetup;
    }
}
