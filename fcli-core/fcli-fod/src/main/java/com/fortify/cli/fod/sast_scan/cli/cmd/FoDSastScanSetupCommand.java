/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod.sast_scan.cli.cmd;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanSetupCommand;
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
    private static final Logger LOG = LoggerFactory.getLogger(FoDSastScanSetupCommand.class);
    @Getter @Mixin private OutputHelperMixins.Setup outputHelper;

    @Option(names = {"--technology-stack"}, required = true, defaultValue = "Auto Detect")
    private String technologyStack;
    @Option(names = {"--language-level"})
    private String languageLevel;
    @Option(names = {"--oss"}, negatable = true)
    private Boolean performOpenSourceAnalysis;
    @Option(names = {"--audit-preference"}, required = true)
    private FoDEnums.AuditPreferenceTypes auditPreferenceType;
    @Option(names = {"--include-third-party-libs"})
    private final Boolean includeThirdPartyLibraries = false;
    @Option(names = {"--use-source-control"})
    private final Boolean useSourceControl = false;
    @Option(names = {"--use-aviator"}, negatable = true)
    private Boolean useAviator;

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

        // Determine technology stack / language level IDs:
        // Technology stack always has a value (defaults to "Auto Detect" if not specified),
        // so we always look it up to get the numeric ID.
        Integer technologyStackId = getTechnologyStackId(unirest);
        Integer languageLevelId = getLanguageLevelId(unirest, technologyStackId, currentSetup);

        var builder = FoDScanConfigSastSetupRequest.builder()
                .entitlementId(entitlementIdToUse)
                .assessmentTypeId(assessmentTypeId)
                .entitlementFrequencyType(entitlementFrequencyTypeMixin.getEntitlementFrequencyType().name())
                .technologyStackId(technologyStackId)
                .auditPreferenceType(auditPreferenceType.name())
                .includeThirdPartyLibraries(includeThirdPartyLibraries)
                .useSourceControl(useSourceControl);

        // Only set languageLevelId if not null
        if (languageLevelId != null) {
            builder.languageLevelId(languageLevelId);
        }

        // OSS value priority: CLI option (if specified) > existing setup value
        if (performOpenSourceAnalysis != null) {
            builder.performOpenSourceAnalysis(performOpenSourceAnalysis);
        } else if (currentSetup != null && currentSetup.getPerformOpenSourceAnalysis() != null) {
            builder.performOpenSourceAnalysis(currentSetup.getPerformOpenSourceAnalysis());
        }

        // Aviator value priority: CLI option (if specified) > existing setup value
        if (useAviator != null) {
            builder.includeFortifyAviator(useAviator);
        } else if (currentSetup != null && currentSetup.getIncludeFortifyAviator() != null) {
            builder.includeFortifyAviator(currentSetup.getIncludeFortifyAviator());
        }

        FoDScanConfigSastSetupRequest setupSastScanRequest = builder.build();

        return FoDScanConfigSastHelper.setupScan(unirest, releaseDescriptor, setupSastScanRequest).asJsonNode();
    }

    private Integer getLanguageLevelId(UnirestInstance unirest, Integer technologyStackId, FoDScanConfigSastDescriptor currentSetup) {
        // If technologyStackId is null, languageLevelId will be null
        if (technologyStackId == null) {
            return null;
        }
        
        // Priority: CLI option > existing setup value (only if tech stack unchanged) > null
        if (languageLevel != null && !languageLevel.isEmpty()) {
            try {
                FoDLookupDescriptor lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupType.LanguageLevels, String.valueOf(technologyStackId), languageLevel, true);
                if (lookupDescriptor != null && lookupDescriptor.getValue() != null) {
                    try {
                        return Integer.valueOf(lookupDescriptor.getValue());
                    } catch (NumberFormatException ex) {
                        throw new FcliTechnicalException("Failed to parse language level ID from lookup descriptor value: " + lookupDescriptor.getValue(), ex);
                    }
                }
                // If lookup returns null, the language level is invalid - return null instead of falling back to currentSetup
                return null;
            } catch (JsonProcessingException ex) {
                throw new FcliTechnicalException("Error processing technology stack lookup", ex);
            }
        } else if (currentSetup != null && currentSetup.getLanguageLevelId() != null 
                   && currentSetup.getTechnologyStackId() != null 
                   && currentSetup.getTechnologyStackId().equals(technologyStackId)) {
            // Only use existing language level if technology stack hasn't changed
            return currentSetup.getLanguageLevelId();
        }
        return null;
    }

    private Integer getTechnologyStackId(UnirestInstance unirest) {
        // find/check technology stack / language level
        FoDLookupDescriptor lookupDescriptor = null;
        try {
            lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupType.TechnologyTypes, technologyStack, true);
        } catch (JsonProcessingException ex) {
            throw new FcliTechnicalException(ex.getMessage());
        }
        if (lookupDescriptor == null) {
            return null;
        }
        try {
            return Integer.valueOf(lookupDescriptor.getValue());
        } catch (NumberFormatException ex) {
            throw new FcliTechnicalException("Failed to parse technology stack ID from lookup descriptor value: " + lookupDescriptor.getValue(), ex);
        }
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
