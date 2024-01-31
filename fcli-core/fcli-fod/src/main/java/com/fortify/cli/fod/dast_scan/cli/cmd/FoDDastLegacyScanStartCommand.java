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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastLegacyHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanStartCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDInProgressScanActionTypeMixins;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDRemediationScanPreferenceTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastLegacyHelper;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastLegacyStartRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastLegacyDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.StartLegacy.CMD_NAME, hidden = true)
public class FoDDastLegacyScanStartCommand extends AbstractFoDScanStartCommand {
    private static final Log LOG = LogFactory.getLog(FoDDastLegacyScanStartCommand.class);
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    @Option(names = {"--assessment-type"}, required = true)
    //private DynamicAssessmentTypes dynamicAssessmentType;
    private String dynamicAssessmentType;
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    @Option(names = {"--start-date"})
    private String startDate;
    @Option(names = {"--notes"})
    private String notes;

    @Mixin
    private FoDRemediationScanPreferenceTypeMixins.OptionalOption remediationScanType;
    @Mixin
    private FoDInProgressScanActionTypeMixins.OptionalOption inProgressScanActionType;
    @Mixin
    private FoDEntitlementFrequencyTypeMixins.RequiredOption entitlementFrequencyTypeMixin;

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
            FoDScanConfigDastLegacyDescriptor currentSetup = FoDScanConfigDastLegacyHelper.getSetupDescriptor(unirest, relId);
            if (currentSetup.getAssessmentTypeId() == null || currentSetup.getAssessmentTypeId() <= 0) {
                throw new IllegalStateException("The dynamic scan configuration for release with id '" + relId +
                        "' has not been setup correctly - 'Assessment Type' is missing or empty.");
            }

            LOG.info("Finding appropriate entitlement to use.");

            // find an appropriate assessment type to use
            Optional<FoDReleaseAssessmentTypeDescriptor> atd = Arrays.stream(
                            FoDReleaseAssessmentTypeHelper.getAssessmentTypes(unirest,
                                    relId, FoDScanType.Dynamic,
                                    entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                                    isRemediation, true)
                    ).filter(n -> n.getName().equals(dynamicAssessmentType))
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
                if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
                    // check if "entitlement id" is already configured
                    if (!Objects.equals(entitlementIdToUse, currentSetup.getEntitlementId())) {
                        LOG.warn("Changing current release entitlement from " + currentSetup.getEntitlementId());
                    }
                }
            }
            LOG.info("Configuring release to use entitlement " + entitlementIdToUse);

            // check if the entitlement is still valid
            FoDReleaseAssessmentTypeHelper.validateEntitlement(relId, atd.get());
            LOG.info("The entitlement " + entitlementIdToUse + " is valid.");

            String startDateStr = (startDate == null || startDate.isEmpty())
                    ? LocalDateTime.now().format(dtf)
                    : LocalDateTime.parse(startDate, dtf).toString();
            FoDScanDastLegacyStartRequest startScanRequest = FoDScanDastLegacyStartRequest.builder()
                    .startDate(startDateStr)
                    .assessmentTypeId(assessmentTypeId)
                    .entitlementId(entitlementIdToUse)
                    .entitlementFrequencyType(entitlementFrequencyTypeMixin.getEntitlementFrequencyType().name())
                    .isRemediationScan(remediationScanType.getRemediationScanPreferenceType() != null && !remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.NonRemediationScanOnly))
                    .applyPreviousScanSettings(true)
                    .scanMethodType("Other")
                    .scanTool(fcliProperties.getProperty("projectName", "fcli"))
                    .scanToolVersion(fcliProperties.getProperty("projectVersion", "unknown")).build();

            //System.out.println(startScanRequest);
            return FoDScanDastLegacyHelper.startScan(unirest, releaseDescriptor, startScanRequest);
        }
    }
}
