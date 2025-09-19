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

import java.util.Objects;

import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanSetupCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.sast.FoDScanSastHelper;
import com.fortify.cli.fod._common.util.FoDEnums;
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
public class FoDSastScanSetupCommand extends AbstractFoDScanSetupCommand<FoDScanConfigSastDescriptor> implements IRecordTransformer {
    private static final Log LOG = LogFactory.getLog(FoDSastScanSetupCommand.class);
    @Getter @Mixin private OutputHelperMixins.Setup outputHelper;

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
    @Option(names={"--use-aviator"})
    private Boolean useAviator = false;

    // TODO We don't actually use a progress writer, but for now we can't
    //      remove the --progress option to maintain backward compatibility.
    //      This should be removed once we move to fcli 3.x (and probably
    //      check other FoD commands as well), unless we actually start
    //      using this of course.
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    @Override
    protected String getScanType() {
        return "Static Assessment";
    }

    @Override
    protected String getSetupType() {
        return auditPreferenceType.name();
    }

    @Override
    protected FoDScanConfigSastDescriptor getSetupDescriptor(UnirestInstance unirest, String releaseId) {
        return FoDScanSastHelper.getSetupDescriptor(unirest, releaseId);
    }

    @Override
    protected boolean isExistingSetup(FoDScanConfigSastDescriptor setupDescriptor) {
        return (setupDescriptor != null && setupDescriptor.getAssessmentTypeId() != 0);
    }

    @Override
    protected ObjectNode convertToObjectNode(FoDScanConfigSastDescriptor setupDescriptor) {
        return setupDescriptor.asObjectNode();
    }

    @Override
    protected JsonNode setup(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanConfigSastDescriptor currentSetup) {
        var relId = releaseDescriptor.getReleaseId();

        LOG.info("Finding appropriate entitlement to use.");

        var atd = FoDReleaseAssessmentTypeHelper.getAssessmentTypeDescriptor(unirest, relId, FoDScanType.Static,
                entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), assessmentType);
        var assessmentTypeId = atd.getAssessmentTypeId();
        var entitlementIdToUse = atd.getEntitlementId();
        assessmentTypeName = atd.getName();

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

        return FoDScanConfigSastHelper.setupScan(unirest, releaseDescriptor, setupSastScanRequest).asJsonNode();
    }

    private Integer getLanguageLevelId(UnirestInstance unirest, Integer technologyStackId) {
        Integer languageLevelId = 0;
        FoDLookupDescriptor lookupDescriptor = null;
        if (languageLevel != null && languageLevel.length() > 0) {
            try {
                lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupType.LanguageLevels, String.valueOf(technologyStackId), languageLevel, true);
            } catch (JsonProcessingException ex) {
                throw new FcliTechnicalException(ex.getMessage());
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
            throw new FcliTechnicalException(ex.getMessage());
        }
        // TODO return 0 or null, or throw exception?
        return lookupDescriptor==null ? 0 : Integer.valueOf(lookupDescriptor.getValue());
    }

    private void validateEntitlement(FoDScanConfigSastDescriptor currentSetup, Integer entitlementIdToUse, String relId, FoDReleaseAssessmentTypeDescriptor atd) {
        // validate entitlement specified or currently in use against assessment type found
        if (entitlementId != null && entitlementId > 0) {
            // check if "entitlement id" explicitly matches what has been found
            if (!Objects.equals(entitlementIdToUse, entitlementId)) {
                throw new FcliSimpleException("Cannot find appropriate assessment type for use with entitlement: " + entitlementId);
            }
        } else {
            if (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0) {
                // check if "entitlement id" is already configured
                if (!Objects.equals(entitlementIdToUse, currentSetup.getEntitlementId())) {
                    LOG.warn("Changing current release entitlement from " + currentSetup.getEntitlementId());
                }
            }
        }
        // check if the entitlement can still be used
        if (FoDReleaseAssessmentTypeHelper.validateEntitlementCanBeUsed(relId, atd)) {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is still valid.");
        } else {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is no longer valid.");
        }
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(getUnirestInstance());
        return ((ObjectNode)record)
        // Start partial fix for (#598)
                        .put("scanType", getScanType())
                        .put("setupType", getSetupType())
        // End               
                        .put("applicationName", releaseDescriptor.getApplicationName())
                        .put("releaseName", releaseDescriptor.getReleaseName())
                        .put("microserviceName", releaseDescriptor.getMicroserviceName());
    }

}
