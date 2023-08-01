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

package com.fortify.cli.fod.scan.cli.cmd.dast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.scan.cli.mixin.FoDAssessmentTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementPreferenceTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDInProgressScanActionTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDRemediationScanPreferenceTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;
import com.fortify.cli.fod.scan.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.dast.FoDDastScanHelper;
import com.fortify.cli.fod.scan.helper.dast.FoDDastScanSetupDescriptor;
import com.fortify.cli.fod.scan.helper.dast.FoDStartDastScanRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.StartDast.CMD_NAME)
public class FoDDastScanStartCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    @Getter @Mixin private FoDOutputHelperMixins.StartDast outputHelper;
    @Mixin private FoDReleaseResolverMixin.PositionalParameter releaseResolver;
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    @Option(names = {"--start-date"})
    private String startDate;
    @Option(names = {"--notes"})
    private String notes;

    @Mixin
    private FoDRemediationScanPreferenceTypeOptions.OptionalOption remediationScanType;
    @Mixin
    private FoDInProgressScanActionTypeOptions.OptionalOption inProgressScanActionType;

    @Mixin
    private FoDEntitlementPreferenceTypeOptions.OptionalOption entitlementType;
    @Mixin
    private FoDAssessmentTypeOptions.OptionalOption assessmentType;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    // TODO Method too long, consider splitting into multiple methods
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try ( var progressWriter = progressWriterFactory.create() ) {
            Properties fcliProperties = FcliBuildPropertiesHelper.getBuildProperties();
            FoDAssessmentTypeDescriptor entitlementToUse = new FoDAssessmentTypeDescriptor();

            FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
            
            // check if scan is already running
            if (releaseDescriptor.getDynamicAnalysisStatusType() != null && (releaseDescriptor.getDynamicAnalysisStatusType().equals("In_Progress")
                    || releaseDescriptor.getDynamicAnalysisStatusType().equals("Scheduled"))) {
                FoDScanDescriptor scanDescriptor = FoDScanHelper.getScanDescriptor(unirest, String.valueOf(releaseDescriptor.getCurrentDynamicScanId()));
                if (inProgressScanActionType.getInProgressScanActionType() != null) {
                    if (inProgressScanActionType.getInProgressScanActionType().equals(FoDEnums.InProgressScanActionType.DoNotStartScan)) {
                        return scanDescriptor.asObjectNode().put("__action__", "SKIPPED_RUNNING");
                    } else if (inProgressScanActionType.getInProgressScanActionType().equals(FoDEnums.InProgressScanActionType.CancelScanInProgress)) {
                        progressWriter.writeWarning("Cancelling scans automatically is not currently supported.");
                    }
                } else {
                    throw new ValidationException("A dynamic scan with id '" + "" + releaseDescriptor.getCurrentDynamicScanId() +
                            "' is already in progress for release: " + releaseDescriptor.getQualifiedName());
                }
            }

            var relId = String.valueOf(releaseDescriptor.getReleaseId());
            // get current setup and check if its valid
            FoDDastScanSetupDescriptor currentSetup = FoDDastScanHelper.getSetupDescriptor(unirest, relId);
            if (StringUtils.isBlank(currentSetup.getDynamicSiteURL())) {
                throw new ValidationException("The dynamic scan configuration for release with id '" + relId +
                        "' has not been setup correctly - 'Dynamic Site URL' is missing or empty.");
            }

            /**
             * Logic for finding/using "entitlement" and "remediation" scanning is as follows:
             *  - if "entitlement id" is specified directly then use it
             *  - if "remediation" scan specified make sure it is valid and available
             *  - if an "assessment type" (Dynamic/Dynamic+) and "entitlement type" (Static/Subscription) then find an
             *    appropriate entitlement to use
             *  - otherwise fall back to current setup
             */
            if (entitlementId != null && entitlementId > 0) {
                entitlementToUse.copyFromCurrentSetup(currentSetup);
                entitlementToUse.setEntitlementId(entitlementId);
            } else if (remediationScanType.getRemediationScanPreferenceType() != null &&
                    (remediationScanType.getRemediationScanPreferenceType() == FoDEnums.RemediationScanPreferenceType.RemediationScanOnly)) {
                // if requesting a remediation scan make we have one available
                entitlementToUse = FoDDastScanHelper.validateRemediationEntitlement(unirest, progressWriter, relId,
                        currentSetup.getEntitlementId(), FoDScanTypeOptions.FoDScanType.Dynamic);
            } else if (assessmentType.getAssessmentType() != null && entitlementType.getEntitlementPreferenceType() != null) {
                // if assessment and entitlement type are both specified, find entitlement to use
                entitlementToUse = FoDDastScanHelper.getEntitlementToUse(unirest, progressWriter, relId,
                        assessmentType.getAssessmentType(), entitlementType.getEntitlementPreferenceType(),
                        FoDScanTypeOptions.FoDScanType.Dynamic);
            } else {
                // use the current scan setup
                entitlementToUse.copyFromCurrentSetup(currentSetup);
            }

            if (entitlementToUse.getEntitlementId() == null || entitlementToUse.getEntitlementId() <= 0) {
                throw new ValidationException("Could not find a valid FoD entitlement to use.");
            }

            String startDateStr = (startDate == null || startDate.isEmpty())
                    ? LocalDateTime.now().format(dtf)
                    : LocalDateTime.parse(startDate, dtf).toString();
            FoDStartDastScanRequest startScanRequest = FoDStartDastScanRequest.builder()
                    .startDate(startDateStr)
                    .assessmentTypeId(entitlementToUse.getAssessmentTypeId())
                    .entitlementId(entitlementToUse.getEntitlementId())
                    .entitlementFrequencyType(entitlementToUse.getFrequencyType())
                    .isRemediationScan(remediationScanType.getRemediationScanPreferenceType() != null && !remediationScanType.getRemediationScanPreferenceType().equals(FoDEnums.RemediationScanPreferenceType.NonRemediationScanOnly))
                    .applyPreviousScanSettings(true)
                    .scanMethodType("Other")
                    .scanTool(fcliProperties.getProperty("projectName", "fcli"))
                    .scanToolVersion(fcliProperties.getProperty("projectVersion", "unknown")).build();

            //System.out.println(startScanRequest);
            return FoDDastScanHelper.startScan(unirest, releaseDescriptor, startScanRequest).asJsonNode();
        }
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDScanHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "STARTED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
