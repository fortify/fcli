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

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDEntitlementFrequencyTypeMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.sast.FoDScanSastHelper;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupType;
import com.fortify.cli.fod.sast_scan.helper.FoDScanConfigSastDescriptor;
import com.fortify.cli.fod.sast_scan.helper.FoDScanConfigSastHelper;
import com.fortify.cli.fod.sast_scan.helper.FoDScanConfigSastSetupRequest;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Setup.CMD_NAME, hidden = false) @CommandGroup("*-scan-setup")
@DisableTest(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT)
public class FoDSastScanSetupCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    private static final Log LOG = LogFactory.getLog(FoDSastScanSetupCommand.class);
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
    @Option(names = {"--technology-stack"}, required = true, defaultValue = "Auto Detect")
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
    @Option(names={"--skip-if-exists"})
    private Boolean skipIfExists = false;
    @Option(names={"--use-aviator"})
    private Boolean useAviator = false;

    // TODO We don't actually use a progress writer, but for now we can't
    //      remove the --progress option to maintain backward compatibility.
    //      This should be removed once we move to fcli 3.x (and probably
    //      check other FoD commands as well), unless we actually start
    //      using this of course.
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try (var progressWriter = progressWriterFactory.create()) {
            var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
            var setupDescriptor = FoDScanSastHelper.getSetupDescriptor(unirest, releaseDescriptor.getReleaseId());
            if ( skipIfExists && setupDescriptor.getAssessmentTypeId()!=0 ) {
                return setupDescriptor.asObjectNode().put("__action__", "SKIPPED_EXISTING");
            } else {
                return setup(unirest, releaseDescriptor, setupDescriptor).asObjectNode();
            }
        }
    }

    private FoDScanConfigSastDescriptor setup(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanConfigSastDescriptor currentSetup) {
        var relId = releaseDescriptor.getReleaseId();

        LOG.info("Finding appropriate entitlement to use.");

        var atd = getAssessmentTypeDescriptor(unirest, relId);
        var assessmentTypeId = atd.getAssessmentTypeId();
        var entitlementIdToUse = atd.getEntitlementId();

        validateEntitlement(currentSetup, entitlementIdToUse, relId, atd);
        LOG.info("Configuring release to use entitlement " + entitlementIdToUse);

        var technologyStackId = getTechnologyStackId(unirest);
        var languageLevelId = getLanguageLevelId(unirest, technologyStackId);

        FoDScanConfigSastSetupRequest setupSastScanRequest = FoDScanConfigSastSetupRequest.builder()
                .entitlementId(entitlementIdToUse)
                .assessmentTypeId(assessmentTypeId)
                .entitlementFrequencyType(entitlementFrequencyTypeMixin.getEntitlementFrequencyType().name())
                .technologyStackId(technologyStackId)
                .languageLevelId(languageLevelId)
                .performOpenSourceAnalysis(performOpenSourceAnalysis)
                .auditPreferenceType(auditPreferenceType.name())
                .includeThirdPartyLibraries(includeThirdPartyLibraries)
                .useSourceControl(useSourceControl)
                .includeFortifyAviator(useAviator).build();

        return FoDScanConfigSastHelper.setupScan(unirest, releaseDescriptor, setupSastScanRequest);
    }

    private Integer getLanguageLevelId(UnirestInstance unirest, Integer technologyStackId) {
        Integer languageLevelId = 0;
        FoDLookupDescriptor lookupDescriptor = null;
        if (languageLevel != null && languageLevel.length() > 0) {
            try {
                lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupType.LanguageLevels, String.valueOf(technologyStackId), languageLevel, true);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex.getMessage());
            }
            if (lookupDescriptor != null) languageLevelId = Integer.valueOf(lookupDescriptor.getValue());
        }
        return languageLevelId;
    }

    private Integer getTechnologyStackId(UnirestInstance unirest) {
        // find/check technology stack / language level
        FoDLookupDescriptor lookupDescriptor = null;
        try {
            lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupType.TechnologyTypes, technologyStack, true);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
        // TODO return 0 or null, or throw exception?
        return lookupDescriptor==null ? 0 : Integer.valueOf(lookupDescriptor.getValue());
    }

    private void validateEntitlement(FoDScanConfigSastDescriptor currentSetup, Integer entitlementIdToUse, String relId, FoDReleaseAssessmentTypeDescriptor atd) {
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
                    LOG.warn("Changing current release entitlement from " + currentSetup.getEntitlementId());
                }
            }
        }
        // check if the entitlement is still valid
        FoDReleaseAssessmentTypeHelper.validateEntitlement(relId, atd);
    }

    private FoDReleaseAssessmentTypeDescriptor getAssessmentTypeDescriptor(UnirestInstance unirest, String relId) {
        // find an appropriate assessment type to use
        Optional<FoDReleaseAssessmentTypeDescriptor> atd = Arrays.stream(
                        FoDReleaseAssessmentTypeHelper.getAssessmentTypes(unirest,
                                relId, FoDScanType.Static,
                                entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                                false, true)
                ).filter(n -> n.getName().equals(staticAssessmentType))
                .findFirst();
        return atd.orElseThrow(()->new IllegalArgumentException("Cannot find appropriate assessment type for specified options."));
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(getUnirestInstance());
        return ((ObjectNode)record)
        // Start partial fix for (#598)
                        .put("scanType", staticAssessmentType)
                        .put("setupType", auditPreferenceType.name())
        // End               
                        .put("applicationName", releaseDescriptor.getApplicationName())
                        .put("releaseName", releaseDescriptor.getReleaseName())
                        .put("microserviceName", releaseDescriptor.getMicroserviceName());
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
