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

package com.fortify.cli.fod.sast_scan.cli.cmd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
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
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupType;
import com.fortify.cli.fod.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanType;
import com.fortify.cli.fod.scan.helper.sast.FoDScanSastHelper;
import com.fortify.cli.fod.sast_scan.helper.FoDScanConfigSastDescriptor;
import com.fortify.cli.fod.sast_scan.helper.FoDScanConfigSastHelper;
import com.fortify.cli.fod.sast_scan.helper.FoDScanConfigSastSetupRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Command(name = OutputHelperMixins.Setup.CMD_NAME, hidden = false)
@DisableTest(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT)
public class FoDSastScanSetupCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Setup outputHelper;

    @Mixin
    private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin
    private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;

    @Option(names = {"--assessment-type"}, required = true)
    private String staticAssessmentType; // Plain text name as custom assessment types can be created
    @Mixin
    private FoDEntitlementFrequencyTypeMixins.RequiredOption entitlementFrequencyTypeMixin;
    @Option(names = {"--entitlement-id"})
    private Integer entitlementId;
    @Option(names = {"--technology-stack"}, required = true)
    private String technologyStack;
    @Option(names = {"--language-level"})
    private String languageLevel;
    @Option(names = {"--oss"})
    private final Boolean performOpenSourceAnalysis = false;
    @Option(names = {"--audit-preference"}, required = true)
    private FoDEnums.AuditPreferenceTypes auditPreferenceType;
    @Option(names = {"--include-third-party-libs"})
    private final Boolean includeThirdPartyLibraries = false;
    @Option(names = {"--use-source-control"})
    private final Boolean useSourceControl = false;

    @Mixin
    private ProgressWriterFactoryMixin progressWriterFactory;

    // TODO Split into multiple methods
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try (var progressWriter = progressWriterFactory.create()) {
            var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
            var relId = releaseDescriptor.getReleaseId();
            Integer entitlementIdToUse = 0;
            Integer assessmentTypeId = 0;
            Integer technologyStackId = 0;
            Integer languageLevelId = 0;

            // get current setup
            FoDScanConfigSastDescriptor currentSetup = FoDScanSastHelper.getSetupDescriptor(unirest, relId);

            progressWriter.writeI18nProgress("fcli.fod.finding-entitlement");

            // find an appropriate assessment type to use
            Optional<FoDAssessmentTypeDescriptor> atd = Arrays.stream(
                            FoDAssessmentTypeHelper.getAssessmentTypes(unirest,
                                    relId, FoDScanType.Static,
                                    entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                                    false, true)
                    ).filter(n -> n.getName().equals(staticAssessmentType))
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
                    throw new IllegalArgumentException("Cannot appropriate assessment type for use with entitlement: " + entitlementId);
                }
            } else {
                if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
                    // check if "entitlement id" is already configured
                    if (!Objects.equals(entitlementIdToUse, currentSetup.getEntitlementId())) {
                        progressWriter.writeI18nWarning("fcli.fod.changing-entitlement");
                    }
                }
            }
            progressWriter.writeI18nProgress("fcli.fod.using-entitlement", entitlementIdToUse);

            // check if the entitlement is still valid
            FoDAssessmentTypeHelper.validateEntitlement(progressWriter, relId, atd.get());
            progressWriter.writeI18nProgress("fcli.fod.valid-entitlement", entitlementIdToUse);

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
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(getUnirestInstance());
        return FoDScanHelper.renameFields(
                ((ObjectNode)record)
                        .put("applicationName", releaseDescriptor.getApplicationName())
                        .put("releaseName", releaseDescriptor.getReleaseName())
                        .put("microserviceName", releaseDescriptor.getMicroserviceName())
        );
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
