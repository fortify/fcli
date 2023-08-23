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

package com.fortify.cli.fod.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.FcliBuildPropertiesHelper;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.assessment_type.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.assessment_type.helper.FoDAssessmentTypeHelper;
import com.fortify.cli.fod.entitlement.helper.FoDEntitlementHelper;
import com.fortify.cli.fod.entitlement.helper.FoDInvalidEntitlementException;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanType;
import com.fortify.cli.fod.scan.helper.mobile.FoDScanMobileHelper;
import com.fortify.cli.fod.scan.helper.mobile.FoDScanMobileStartRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Command(name = FoDOutputHelperMixins.StartMobile.CMD_NAME)
public class FoDScanStartMobileCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    @Getter @Mixin private FoDOutputHelperMixins.StartMobile outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.PositionalParameter releaseResolver;
    private enum MobileAssessmentTypes { Mobile, MobilePlus, Remediation }
    @Option(names = {"--assessment-type"}, required = true)
    //private MobileAssessmentTypes mobileAssessmentType;
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
    @Option(names = {"-f", "--file"}, required = true)
    private File scanFile;
    @Mixin private FoDEntitlementFrequencyTypeMixins.RequiredOption entitlementFrequencyTypeMixin;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    // TODO Split into multiple methods
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try ( var progressWriter = progressWriterFactory.create() ) {
            Properties fcliProperties = FcliBuildPropertiesHelper.getBuildProperties();
            var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
            String relId = releaseDescriptor.getReleaseId();
            Integer entitlementIdToUse = 0;
            Integer assessmentTypeId = 0;

            // get current setup
            // NOTE: there is currently no GET method for retrieving scan setup so the following cannot be used:
            // FoDMobileScanSetupDescriptor foDMobileScanSetupDescriptor = FoDMobileScanHelper.getSetupDescriptor(unirest, relId);

            // find/check out assessment type id
            FoDAssessmentTypeDescriptor[] appRelAssessmentTypeDescriptor = FoDAssessmentTypeHelper.getAssessmentTypes(unirest, relId, FoDScanType.Mobile,
                    entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), true);
            for (FoDAssessmentTypeDescriptor assessmentType : appRelAssessmentTypeDescriptor) {
                if (assessmentType.getName().equals(mobileAssessmentType)) {
                    assessmentTypeId = assessmentType.getAssessmentTypeId();
                }
            }
            if (assessmentTypeId == 0) {
                throw new IllegalArgumentException("Cannot find assessment type with name '" + mobileAssessmentType + "'");
            }

            // find/validate entitlement
            if (entitlementId != null && entitlementId > 0) {
                // use "entitlement id" explicitly specified
                entitlementIdToUse = entitlementId;
// NOTE: cannot be used
//            } else if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
//                // use "entitlement id" already configured
//                entitlementIdToUse = currentSetup.getEntitlementId();
//                progressWriter.writeI18nProgress("fcli.fod.scan.start-mobile.finding-entitlement");
            } else {
                // find an appropriate "entitlement id" to use
                entitlementIdToUse = FoDScanHelper.findEntitlementIdToUse(unirest, progressWriter, relId, mobileAssessmentType,
                        entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                        FoDScanType.Mobile);
            }
            // validate the entitlement
            try {
                FoDEntitlementHelper.validateEntitlement(unirest, progressWriter, entitlementIdToUse);
            } catch (FoDInvalidEntitlementException ex) {
                throw new IllegalStateException(ex.getMessage());
            }
            progressWriter.writeI18nProgress("fcli.fod.scan.start-mobile.using-entitlement", entitlementIdToUse);

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

            return FoDScanMobileHelper.startScan(unirest, progressWriter, releaseDescriptor, startScanRequest, scanFile).asJsonNode();
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
