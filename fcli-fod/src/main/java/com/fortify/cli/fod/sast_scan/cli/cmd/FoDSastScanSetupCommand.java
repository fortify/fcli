/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.sast_scan.cli.cmd;

import javax.validation.ValidationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.spi.transform.IRecordTransformer;
import com.fortify.cli.fod.lookup.cli.mixin.FoDLookupTypeOptions;
import com.fortify.cli.fod.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.output.cli.AbstractFoDOutputCommand;
import com.fortify.cli.fod.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDAppMicroserviceRelResolverMixin;
import com.fortify.cli.fod.release.helper.FoDAppRelAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.sast_scan.helper.FoDSastScanHelper;
import com.fortify.cli.fod.sast_scan.helper.FoDSastScanSetupDescriptor;
import com.fortify.cli.fod.sast_scan.helper.FoDSetupSastScanRequest;
import com.fortify.cli.fod.scan.cli.mixin.FoDAssessmentTypeOptions;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanFormatOptions;
import com.fortify.cli.fod.scan.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.util.FoDEnums;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = FoDOutputHelperMixins.Setup.CMD_NAME)
public class FoDSastScanSetupCommand extends AbstractFoDOutputCommand implements IUnirestJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private FoDOutputHelperMixins.Create outputHelper;
    @Mixin
    private FoDAppMicroserviceRelResolverMixin.PositionalParameter appMicroserviceRelResolver;

    @Option(names = {"--entitlement-frequency", "--frequency"})
    private FoDEnums.EntitlementFrequencyTypes entitlementFrequency;
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

    @Mixin
    private FoDAssessmentTypeOptions.RequiredOption assessmentType;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {

        String relId = appMicroserviceRelResolver.getAppMicroserviceRelId(unirest);
        Integer entitlementIdToUse = 0;
        Integer assessmentTypeId = 0;
        Integer technologyStackId = 0;
        Integer languageLevelId = 0;

        // get current setup
        FoDSastScanSetupDescriptor currentSetup = FoDSastScanHelper.getSetupDescriptor(unirest, relId);

        // find/check out assessment type id
        FoDScanFormatOptions.FoDScanType scanType = assessmentType.getAssessmentType().toScanType();
        FoDAppRelAssessmentTypeDescriptor[] appRelAssessmentTypeDescriptor = FoDAppRelHelper.getAppRelAssessmentTypes(unirest, relId,
                scanType, true);

        String assessmentTypeName = assessmentType.getAssessmentType().toString().replace("Plus", "+") + " Assessment";
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
            if (entitlementFrequency == FoDEnums.EntitlementFrequencyTypes.SingleScan) {
                entitlementPreferenceType = FoDEnums.EntitlementPreferenceType.SingleScanOnly;
            } else if (entitlementFrequency == FoDEnums.EntitlementFrequencyTypes.Subscription) {
                entitlementPreferenceType = FoDEnums.EntitlementPreferenceType.SubscriptionOnly;
            } else {
                throw new ValidationException("The entitlement frequency '"
                        + entitlementFrequency.name() + "' cannot be used here");
            }
            FoDAssessmentTypeDescriptor assessmentTypeDescriptor = FoDScanHelper.getEntitlementToUse(unirest, relId, assessmentType.getAssessmentType(), entitlementPreferenceType, scanType);
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
