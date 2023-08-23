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

package com.fortify.cli.fod.scan_config.cli.cmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.assessment_type.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.assessment_type.helper.FoDAssessmentTypeHelper;
import com.fortify.cli.fod.entitlement.helper.FoDEntitlementHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupType;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod.entitlement.helper.FoDInvalidEntitlementException;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanType;
import com.fortify.cli.fod.scan.helper.sast.FoDScanSastHelper;
import com.fortify.cli.fod.scan_config.helper.FoDScanConfigSastDescriptor;
import com.fortify.cli.fod.scan_config.helper.FoDScanConfigSastHelper;
import com.fortify.cli.fod.scan_config.helper.FoDScanConfigSastSetupRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.SetupSast.CMD_NAME)
@DisableTest(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT)
public class FoDScanConfigSetupSastCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.SetupSast outputHelper;

    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.PositionalParameter releaseResolver;

    @Option(names = {"--assessment-type"}, required = true)
    private String staticAssessmentType;
    @Mixin private FoDEntitlementFrequencyTypeMixins.RequiredOption entitlementFrequencyTypeMixin;
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    @Option(names = {"--technology-stack"}, required = true)
    private String technologyStack;
    @Option(names = {"--language-level"})
    private String languageLevel;
    @Option(names = {"--oss"})
    private Boolean performOpenSourceAnalysis = false;
    @Option(names = {"--audit-preference"}, required = true)
    private FoDEnums.AuditPreferenceTypes auditPreferenceType;
    @Option(names = {"--include-third-party-libs"})
    private Boolean includeThirdPartyLibraries = false;
    @Option(names = {"--use-source-control"})
    private Boolean useSourceControl = false;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    // TODO Split into multiple methods
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try ( var progressWriter = progressWriterFactory.create() ) {
            var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
            var relId = releaseDescriptor.getReleaseId();
            Integer entitlementIdToUse = 0;
            Integer assessmentTypeId = 0;
            Integer technologyStackId = 0;
            Integer languageLevelId = 0;

            // get current setup
            FoDScanConfigSastDescriptor currentSetup = FoDScanSastHelper.getSetupDescriptor(unirest, relId);

            // find/check out assessment type id
            FoDAssessmentTypeDescriptor[] appRelAssessmentTypeDescriptor = FoDAssessmentTypeHelper.getAssessmentTypes(unirest, relId, FoDScanType.Static,
                    entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), true);
            for (FoDAssessmentTypeDescriptor assessmentType : appRelAssessmentTypeDescriptor) {
                if (assessmentType.getName().equals(staticAssessmentType)) {
                    assessmentTypeId = assessmentType.getAssessmentTypeId();
                }
            }
            if (assessmentTypeId == 0) {
                throw new IllegalArgumentException("Cannot find assessment type with name '" + staticAssessmentType + "'");
            }
            //System.out.println("assessmentTypeId = " + assessmentTypeId);

            // find/validate entitlement
            if (entitlementId != null && entitlementId > 0) {
                // use "entitlement id" explicitly specified
                entitlementIdToUse = entitlementId;
            } else if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
                // use "entitlement id" already configured
                entitlementIdToUse = currentSetup.getEntitlementId();
                progressWriter.writeI18nProgress("fcli.fod.scan-config.setup-sast.finding-entitlement");
            } else {
                // find an appropriate "entitlement id" to use
                entitlementIdToUse = FoDScanHelper.findEntitlementIdToUse(unirest, progressWriter, relId, staticAssessmentType,
                        entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                        FoDScanType.Static);
            }
            // validate the entitlement
            try {
                FoDEntitlementHelper.validateEntitlement(unirest, progressWriter, entitlementIdToUse);
            } catch (FoDInvalidEntitlementException ex) {
                throw new IllegalStateException(ex.getMessage());
            }
            progressWriter.writeI18nProgress("fcli.fod.scan-config.setup-sast.using-entitlement", entitlementIdToUse);

            // find/check technology stack / language level
            FoDLookupDescriptor lookupDescriptor = null;
            try {
                lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupType.TechnologyTypes, technologyStack, true);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex.getMessage());
            }
            if (lookupDescriptor != null) technologyStackId = Integer.valueOf(lookupDescriptor.getValue());
            //System.out.println("technologyStackId = " + technologyStackId);
            if (languageLevel != null && languageLevel.length() > 0) {
                try {
                    lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupType.LanguageLevels, String.valueOf(technologyStackId), languageLevel, true);
                } catch (JsonProcessingException ex) {
                    throw new IllegalStateException(ex.getMessage());
                }
                if (lookupDescriptor != null) languageLevelId = Integer.valueOf(lookupDescriptor.getValue());
                //System.out.println("languageLevelId = " + languageLevelId);
            }

            FoDScanConfigSastSetupRequest setupSastScanRequest = FoDScanConfigSastSetupRequest.builder()
                    .entitlementId(entitlementIdToUse)
                    .assessmentTypeId(assessmentTypeId)
                    .entitlementFrequencyType(entitlementFrequencyTypeMixin.getEntitlementFrequencyType().name())
                    .technologyStackId(technologyStackId)
                    .languageLevelId(languageLevelId)
                    .performOpenSourceAnalysis(performOpenSourceAnalysis)
                    .auditPreferenceType(auditPreferenceType.name())
                    .includeThirdPartyLibraries(includeThirdPartyLibraries)
                    .useSourceControl(useSourceControl).build();

            return FoDScanConfigSastHelper.setupScan(unirest, releaseDescriptor, setupSastScanRequest).asJsonNode();
        }
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return FoDScanHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "SETUP";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
