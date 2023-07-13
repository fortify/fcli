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

package com.fortify.cli.fod.scan.cli.cmd.mobile;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import javax.validation.ValidationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.util.FoDConstants;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.lookup.cli.mixin.FoDLookupTypeOptions;
import com.fortify.cli.fod.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDAppMicroserviceRelResolverMixin;
import com.fortify.cli.fod.scan.cli.mixin.FoDAssessmentTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementFrequencyTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;
import com.fortify.cli.fod.scan.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.mobile.FoDMobileScanHelper;
import com.fortify.cli.fod.scan.helper.mobile.FoDStartMobileScanRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.StartMobile.CMD_NAME)
public class FoDMobileScanStartCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    @Getter @Mixin private FoDOutputHelperMixins.StartMobile outputHelper;
    @Mixin
    private FoDAppMicroserviceRelResolverMixin.PositionalParameter appMicroserviceRelResolver;
    private enum MobileAssessmentTypes { Mobile, MobilePlus, Remediation }
    @Option(names = {"--assessment-type"}, required = true)
    private MobileAssessmentTypes mobileAssessmentType;
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
    @Option(names = {"--chunk-size"})
    private int chunkSize = FoDConstants.DEFAULT_CHUNK_SIZE;
    @Option(names = {"-f", "--file"}, required = true)
    private File scanFile;

    @Mixin
    private FoDEntitlementFrequencyTypeOptions.RequiredOption entitlementType;
    // no longer used - using specific MobileAssessmentTypes above
    //@Mixin
    //private FoDAssessmentTypeOptions.OptionalOption assessmentType;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    // TODO Split into multiple methods
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try ( var progressWriter = progressWriterFactory.create() ) {
            Properties fcliProperties = FcliBuildPropertiesHelper.getBuildProperties();
            String relId = appMicroserviceRelResolver.getAppMicroserviceRelId(unirest);

            // retrieve current scan setup
            // NOTE: there is currently no GET method for retrieving scan setup so the following cannot be used:
            // FoDMobileScanSetupDescriptor foDMobileScanSetupDescriptor = FoDMobileScanHelper.getSetupDescriptor(unirest, relId);

            // TODO: check if a scan is already running

            // get entitlement to use
            FoDAssessmentTypeDescriptor entitlementToUse = getEntitlementToUse(unirest, progressWriter, relId);

            // validate timezone (if specified)
            String timeZoneToUse = validateTimezone(unirest, timezone);

            String startDateStr = (startDate == null || startDate.isEmpty())
                    ? LocalDateTime.now().format(dtf)
                    : LocalDateTime.parse(startDate, dtf).toString();

            FoDStartMobileScanRequest startScanRequest = FoDStartMobileScanRequest.builder()
                    .startDate(startDateStr)
                    .assessmentTypeId(entitlementToUse.getAssessmentTypeId())
                    .entitlementId(entitlementToUse.getEntitlementId())
                    .entitlementFrequencyType(entitlementToUse.getFrequencyType())
                    .timeZone(timeZoneToUse)
                    .frameworkType(mobileFramework.name())
                    .scanMethodType("Other")
                    .notes(notes != null && !notes.isEmpty() ? notes : "")
                    .scanTool(fcliProperties.getProperty("projectName", "fcli"))
                    .scanToolVersion(fcliProperties.getProperty("projectVersion", "unknown")).build();

            return FoDMobileScanHelper.startScan(unirest, progressWriter, relId, startScanRequest, scanFile, chunkSize).asJsonNode();
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

    private FoDAssessmentTypeDescriptor getEntitlementToUse(UnirestInstance unirest, IProgressWriterI18n progressWriter, String relId) {
        FoDAssessmentTypeDescriptor entitlementToUse = new FoDAssessmentTypeDescriptor();

        /**
         * Logic for finding/using "entitlement" is as follows:
         *  - if "entitlement id" is specified directly then use it
         *  - if an "assessment type" (Mobile/Mobile+) and "entitlement type" (Single/Subscription) then find an appropriate entitlement to use
         *  - otherwise fail
         */
        if (entitlementId != null && entitlementId > 0) {
            entitlementToUse.setEntitlementId(entitlementId);
        }

        // if assessment and entitlement type are both specified, find entitlement to use
        FoDAssessmentTypeOptions.FoDAssessmentType assessmentType = FoDAssessmentTypeOptions.FoDAssessmentType.valueOf(String.valueOf(mobileAssessmentType));
        FoDEnums.EntitlementPreferenceType entitlementPreferenceType = FoDEnums.EntitlementPreferenceType.fromInt(entitlementType.getEntitlementFrequencyType().getValue());
        entitlementToUse = FoDMobileScanHelper.getEntitlementToUse(unirest, progressWriter, relId,
                assessmentType, entitlementPreferenceType,
                FoDScanTypeOptions.FoDScanType.Mobile);

        if (entitlementToUse.getEntitlementId() == null || entitlementToUse.getEntitlementId() <= 0) {
            throw new ValidationException("Could not find a valid FoD entitlement to use.");
        }
        return entitlementToUse;
    }

    private String validateTimezone(UnirestInstance unirest, String timezone) {
        FoDLookupDescriptor lookupDescriptor = null;
        if (timezone != null && !timezone.isEmpty()) {
            try {
                lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupTypeOptions.FoDLookupType.TimeZones, timezone, false);
            } catch (JsonProcessingException ex) {
                throw new ValidationException(ex.getMessage());
            }
            return lookupDescriptor.getValue();
        } else {
            // default to UTC
            return "UTC";
        }
    }

}
