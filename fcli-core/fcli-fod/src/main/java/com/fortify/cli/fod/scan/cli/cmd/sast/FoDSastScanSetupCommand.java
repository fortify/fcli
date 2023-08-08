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

package com.fortify.cli.fod.scan.cli.cmd.sast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;
import com.fortify.cli.fod.rest.lookup.cli.mixin.FoDLookupTypeOptions;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod.scan.helper.FoDAssessmentType;
import com.fortify.cli.fod.scan.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanType;
import com.fortify.cli.fod.scan.helper.sast.FoDSastScanHelper;
import com.fortify.cli.fod.scan.helper.sast.FoDSastScanSetupDescriptor;
import com.fortify.cli.fod.scan.helper.sast.FoDSetupSastScanRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.SetupSast.CMD_NAME)
public class FoDSastScanSetupCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.SetupSast outputHelper;
    
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.PositionalParameter releaseResolver;

    private enum StaticAssessmentTypes { Static, StaticPlus }
    @Option(names = {"--assessment-type"}, required = true)
    private StaticAssessmentTypes staticAssessmentType;
    @Mixin private FoDEntitlementFrequencyTypeMixins.OptionalOption entitlementFrequencyTypeMixin;
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

            // TODO Unused variable
            // get current setup
            FoDSastScanSetupDescriptor currentSetup = FoDSastScanHelper.getSetupDescriptor(unirest, relId);

            // find/check out assessment type id
            //FoDScanTypeOptions.FoDScanType scanType = assessmentType.getAssessmentType().toScanType();
            FoDReleaseAssessmentTypeDescriptor[] appRelAssessmentTypeDescriptor = FoDReleaseHelper.getAppRelAssessmentTypes(unirest, relId,
                    FoDScanType.Static, true);
            //String assessmentTypeName = assessmentType.getAssessmentType().toString().replace("Plus", "+") + " Assessment";
            String assessmentTypeName = staticAssessmentType.name().replace("Plus", "+") + " Assessment";
            for (FoDReleaseAssessmentTypeDescriptor assessmentType : appRelAssessmentTypeDescriptor) {
                if (assessmentType.getName().equals(assessmentTypeName)) {
                    assessmentTypeId = assessmentType.getAssessmentTypeId();
                }
            }
            //System.out.println("assessmentTypeId = " + assessmentTypeId);

            // find/check entitlement id
            if (entitlementId != null && entitlementId > 0) {
                entitlementIdToUse = entitlementId;
                // TODO: verify entitlementId
            } else {
                FoDEnums.EntitlementPreferenceType entitlementPreferenceType = null;
                var entitlementFrequency = entitlementFrequencyTypeMixin.getEntitlementFrequencyType();               
                if (entitlementFrequency == FoDEnums.EntitlementFrequencyType.SingleScan) {
                    entitlementPreferenceType = FoDEnums.EntitlementPreferenceType.SingleScanOnly;
                } else if (entitlementFrequency == FoDEnums.EntitlementFrequencyType.Subscription) {
                    entitlementPreferenceType = FoDEnums.EntitlementPreferenceType.SubscriptionOnly;
                } else {
                    throw new IllegalArgumentException("The entitlement frequency '"
                            + entitlementFrequency.name() + "' cannot be used here");
                }
                FoDAssessmentType assessmentType = FoDAssessmentType.valueOf(String.valueOf(staticAssessmentType));
                FoDAssessmentTypeDescriptor assessmentTypeDescriptor = FoDScanHelper.getEntitlementToUse(unirest, progressWriter, relId,
                        assessmentType, entitlementPreferenceType,
                        FoDScanType.Mobile);
                entitlementIdToUse = assessmentTypeDescriptor.getEntitlementId();
            }
            //System.out.println("entitlementId = " + entitlementIdToUse);

            // find/check technology stack / language level
            FoDLookupDescriptor lookupDescriptor = null;
            try {
                lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupTypeOptions.FoDLookupType.TechnologyTypes, technologyStack, true);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex.getMessage());
            }
            if (lookupDescriptor != null) technologyStackId = Integer.valueOf(lookupDescriptor.getValue());
            //System.out.println("technologyStackId = " + technologyStackId);
            if (languageLevel != null && languageLevel.length() > 0) {
                try {
                    lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupTypeOptions.FoDLookupType.LanguageLevels, String.valueOf(technologyStackId), languageLevel, true);
                } catch (JsonProcessingException ex) {
                    throw new IllegalStateException(ex.getMessage());
                }
                if (lookupDescriptor != null) languageLevelId = Integer.valueOf(lookupDescriptor.getValue());
                //System.out.println("languageLevelId = " + languageLevelId);
            }

            FoDSetupSastScanRequest setupSastScanRequest = FoDSetupSastScanRequest.builder()
                .entitlementId(entitlementIdToUse)
                .assessmentTypeId(assessmentTypeId)
                .entitlementFrequencyType(entitlementFrequencyTypeMixin.getEntitlementFrequencyType().name())
                .technologyStackId(technologyStackId)
                .languageLevelId(languageLevelId)
                .performOpenSourceAnalysis(performOpenSourceAnalysis)
                .auditPreferenceType(auditPreferenceType.name())
                .includeThirdPartyLibraries(includeThirdPartyLibraries)
                .useSourceControl(useSourceControl).build();

            return FoDSastScanHelper.setupScan(unirest, releaseDescriptor, setupSastScanRequest).asJsonNode();
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
