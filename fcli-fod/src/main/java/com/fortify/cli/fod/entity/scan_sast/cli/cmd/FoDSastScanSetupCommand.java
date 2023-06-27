/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/

package com.fortify.cli.fod.entity.scan_sast.cli.cmd;

import javax.validation.ValidationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.fod.entity.lookup.cli.mixin.FoDLookupTypeOptions;
import com.fortify.cli.fod.entity.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.entity.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.entity.release.cli.mixin.FoDAppMicroserviceRelResolverMixin;
import com.fortify.cli.fod.entity.release.helper.FoDAppRelAssessmentTypeDescriptor;
import com.fortify.cli.fod.entity.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.entity.scan.cli.mixin.FoDAssessmentTypeOptions;
import com.fortify.cli.fod.entity.scan.cli.mixin.FoDScanTypeOptions;
import com.fortify.cli.fod.entity.scan.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.entity.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.entity.scan_sast.helper.FoDSastScanHelper;
import com.fortify.cli.fod.entity.scan_sast.helper.FoDSastScanSetupDescriptor;
import com.fortify.cli.fod.entity.scan_sast.helper.FoDSetupSastScanRequest;
import com.fortify.cli.fod.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.util.FoDEnums;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.SetupSast.CMD_NAME)
public class FoDSastScanSetupCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.SetupSast outputHelper;
    @Mixin
    private FoDAppMicroserviceRelResolverMixin.PositionalParameter appMicroserviceRelResolver;

    private enum StaticAssessmentTypes { Static, StaticPlus }
    @Option(names = {"--assessment-type"}, required = true)
    private StaticAssessmentTypes staticAssessmentType;
    @Option(names = {"--entitlement-frequency", "--frequency"}, required = true)
    private FoDEnums.EntitlementFrequencyType entitlementFrequency;
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
//    @Option(names = {"--scan-binary"})
//    private Boolean scanBinary = false;

    // no longer used - using specific StaticAssessmentTypes above
    //@Mixin
    //private FoDAssessmentTypeOptions.RequiredOption assessmentType;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    // TODO Split into multiple methods
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try ( var progressWriter = progressWriterFactory.create() ) {
            String relId = appMicroserviceRelResolver.getAppMicroserviceRelId(unirest);
            Integer entitlementIdToUse = 0;
            Integer assessmentTypeId = 0;
            Integer technologyStackId = 0;
            Integer languageLevelId = 0;

            // TODO Unused variable
            // get current setup
            FoDSastScanSetupDescriptor currentSetup = FoDSastScanHelper.getSetupDescriptor(unirest, relId);

            // find/check out assessment type id
            //FoDScanTypeOptions.FoDScanType scanType = assessmentType.getAssessmentType().toScanType();
            FoDAppRelAssessmentTypeDescriptor[] appRelAssessmentTypeDescriptor = FoDAppRelHelper.getAppRelAssessmentTypes(unirest, relId,
                    FoDScanTypeOptions.FoDScanType.Static, true);
            //String assessmentTypeName = assessmentType.getAssessmentType().toString().replace("Plus", "+") + " Assessment";
            String assessmentTypeName = staticAssessmentType.name().replace("Plus", "+") + " Assessment";
            for (FoDAppRelAssessmentTypeDescriptor assessmentType : appRelAssessmentTypeDescriptor) {
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
                if (entitlementFrequency == FoDEnums.EntitlementFrequencyType.SingleScan) {
                    entitlementPreferenceType = FoDEnums.EntitlementPreferenceType.SingleScanOnly;
                } else if (entitlementFrequency == FoDEnums.EntitlementFrequencyType.Subscription) {
                    entitlementPreferenceType = FoDEnums.EntitlementPreferenceType.SubscriptionOnly;
                } else {
                    throw new ValidationException("The entitlement frequency '"
                            + entitlementFrequency.name() + "' cannot be used here");
                }
                FoDAssessmentTypeOptions.FoDAssessmentType assessmentType = FoDAssessmentTypeOptions.FoDAssessmentType.valueOf(String.valueOf(staticAssessmentType));
                FoDAssessmentTypeDescriptor assessmentTypeDescriptor = FoDScanHelper.getEntitlementToUse(unirest, progressWriter, relId,
                        assessmentType, entitlementPreferenceType,
                        FoDScanTypeOptions.FoDScanType.Mobile);
                entitlementIdToUse = assessmentTypeDescriptor.getEntitlementId();
            }
            //System.out.println("entitlementId = " + entitlementIdToUse);

            // find/check technology stack / language level
            FoDLookupDescriptor lookupDescriptor = null;
            try {
                lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupTypeOptions.FoDLookupType.TechnologyTypes, technologyStack, true);
            } catch (JsonProcessingException ex) {
                throw new ValidationException(ex.getMessage());
            }
            if (lookupDescriptor != null) technologyStackId = Integer.valueOf(lookupDescriptor.getValue());
            //System.out.println("technologyStackId = " + technologyStackId);
            if (languageLevel != null && languageLevel.length() > 0) {
                try {
                    lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupTypeOptions.FoDLookupType.LanguageLevels, String.valueOf(technologyStackId), languageLevel, true);
                } catch (JsonProcessingException ex) {
                    throw new ValidationException(ex.getMessage());
                }
                if (lookupDescriptor != null) languageLevelId = Integer.valueOf(lookupDescriptor.getValue());
                //System.out.println("languageLevelId = " + languageLevelId);
            }

            FoDSetupSastScanRequest setupSastScanRequest = new FoDSetupSastScanRequest()
                .setEntitlementId(entitlementIdToUse)
                .setAssessmentTypeId(assessmentTypeId)
                .setEntitlementFrequencyType(entitlementFrequency.name())
                .setTechnologyStackId(technologyStackId)
                .setLanguageLevelId(languageLevelId)
                .setPerformOpenSourceAnalysis(performOpenSourceAnalysis)
                .setAuditPreferenceType(auditPreferenceType.name())
                .setIncludeThirdPartyLibraries(includeThirdPartyLibraries)
                .setUseSourceControl(useSourceControl);

            return FoDSastScanHelper.setupScan(unirest, Integer.valueOf(relId), setupSastScanRequest).asJsonNode();
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
