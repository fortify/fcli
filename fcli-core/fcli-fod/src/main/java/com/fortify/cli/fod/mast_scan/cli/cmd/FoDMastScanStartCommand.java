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

package com.fortify.cli.fod.mast_scan.cli.cmd;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanStartCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDRemediationScanPreferenceTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.mobile.FoDScanMobileHelper;
import com.fortify.cli.fod._common.scan.helper.mobile.FoDScanMobileStartRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
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
    private static final Log LOG = LogFactory.getLog(FoDMastScanStartCommand.class);
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    @Option(names = {"--assessment-type"}, required = true)
    private String mobileAssessmentType;
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    private enum MobileFrameworks { iOS, Android }
    @Option(names = {"--framework"}, required = true)
    private MobileFrameworks mobileFramework;
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
        try ( var progressWriter = progressWriterFactory.create() ) {
            Properties fcliProperties = FcliBuildPropertiesHelper.getBuildProperties();
            String relId = releaseDescriptor.getReleaseId();
            Integer entitlementIdToUse = 0;
            Integer assessmentTypeId = 0;
            Boolean isRemediation = false;

            // if we have requested remediation scan use it to find appropriate assessment type
            if (remediationScanType != null && remediationScanType.getRemediationScanPreferenceType() != null) {
                if (remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.RemediationScanIfAvailable) ||
                        remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.RemediationScanOnly)) {
                    isRemediation = true;
                }
            }

            // get current setup
            // NOTE: there is currently no GET method for retrieving scan setup so the following cannot be used:
            // FoDMobileScanSetupDescriptor foDMobileScanSetupDescriptor = FoDMobileScanHelper.getSetupDescriptor(unirest, relId);

            LOG.info("Finding appropriate entitlement to use.");

            // find an appropriate assessment type to use
            Optional<FoDReleaseAssessmentTypeDescriptor> atd = Arrays.stream(
                            FoDReleaseAssessmentTypeHelper.getAssessmentTypes(unirest,
                                    relId, FoDScanType.Mobile,
                                    entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                                    isRemediation, true)
                    ).filter(n -> n.getName().equals(mobileAssessmentType))
                    .findFirst();
            if (atd.isEmpty()) {
                throw new IllegalArgumentException("Cannot find appropriate assessment type for specified options.");
            }
            assessmentTypeId = atd.get().getAssessmentTypeId();
            entitlementIdToUse = atd.get().getEntitlementId();

            // validate entitlement specified or currently in use against assessment type found
            if (entitlementId != null && entitlementId > 0) {
                // check if "entitlement id" explicitly matches what has been found
                if (!Objects.equals(entitlementIdToUse, entitlementId)) {
                    throw new IllegalArgumentException("Cannot find appropriate assessment type with entitlement: " + entitlementId);
                }
            } else {
                // NOTE: there is currently no GET method for retrieving scan setup so the following cannot be used:
                //if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
                //    // check if "entitlement id" is already configured
                //    if (!Objects.equals(entitlementIdToUse, currentSetup.getEntitlementId())) {
                //        progressWriter.writeI18nWarning("fcli.fod.scan-config.setup-sast.changing-entitlement");
                //    }
                // }
            }
            LOG.info("Configuring release to use entitlement " + entitlementIdToUse);

            // check if the entitlement is still valid
            FoDReleaseAssessmentTypeHelper.validateEntitlement(relId, atd.get());
            LOG.info("The entitlement " + entitlementIdToUse + " is valid");

            // validate timezone (if specified)
            String timeZoneToUse = FoDScanHelper.validateTimezone(unirest, timezone);

            String startDateStr = (startDate == null || startDate.isEmpty())
                    ? LocalDateTime.now().format(dtf)
                    : LocalDateTime.parse(startDate, dtf).toString();

            FoDScanMobileStartRequest startScanRequest = FoDScanMobileStartRequest.builder()
                    .startDate(startDateStr)
                    .assessmentTypeId(assessmentTypeId)
                    .entitlementId(entitlementIdToUse)
                    .entitlementFrequencyType(entitlementFrequencyTypeMixin.getEntitlementFrequencyType().name())
                    .timeZone(timeZoneToUse)
                    .frameworkType(mobileFramework.name())
                    .scanMethodType("Other")
                    .notes(notes != null && !notes.isEmpty() ? notes : "")
                    .scanTool(fcliProperties.getProperty("projectName", "fcli"))
                    .scanToolVersion(fcliProperties.getProperty("projectVersion", "unknown")).build();

            return FoDScanMobileHelper.startScan(unirest, progressWriter, releaseDescriptor, startScanRequest, scanFileMixin.getFile());
        }
    }
}
