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

package com.fortify.cli.fod.mast_scan.cli.cmd;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanSetupCommand;
import com.fortify.cli.fod._common.scan.helper.mobile.FoDScanMobileHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileDescriptor;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileHelper;
import com.fortify.cli.fod.mast_scan.helper.FoDScanConfigMobileSetupRequest;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Setup.CMD_NAME, hidden = false) @CommandGroup("*-scan-setup")
@DisableTest(TestType.CMD_DEFAULT_TABLE_OPTIONS_PRESENT)
public class FoDMastScanSetupCommand extends AbstractFoDScanSetupCommand<FoDScanConfigMobileDescriptor> implements IRecordTransformer, IActionCommandResultSupplier {
    private static final Log LOG = LogFactory.getLog(FoDMastScanSetupCommand.class);
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;

    private enum MobileFrameworks { iOS, Android }
    @Option(names = {"--framework"}, required = true, defaultValue = "Android")
    private MobileFrameworks mobileFramework;
    @Option(names = {"--timezone"}, defaultValue = "UTC")
    private String timezone;
    private enum MobileAuditPreferenceTypes { Manual, Automated }
    @Option(names = {"--audit-preference"}, defaultValue = "Automated")
    private MobileAuditPreferenceTypes auditPreferenceType;
    private enum MobilePlatforms { Phone, Tablet, Both }
    @Option(names = {"--platform"}, defaultValue = "Phone")
    private MobilePlatforms mobilePlatform;

    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;

    @Override
    protected String getScanType() {
        return "Mobile Assessment";
    }

    @Override
    protected String getSetupType() {
        return auditPreferenceType.name();
    }

    @Override
    protected FoDScanConfigMobileDescriptor getSetupDescriptor(UnirestInstance unirest, String releaseId) {
        return FoDScanMobileHelper.getSetupDescriptor(unirest, releaseId);
    }

    @Override
    protected boolean isExistingSetup(FoDScanConfigMobileDescriptor setupDescriptor) {
        return (setupDescriptor != null && setupDescriptor.getAssessmentTypeId() != 0);
    }

    @Override
    protected ObjectNode convertToObjectNode(FoDScanConfigMobileDescriptor setupDescriptor) {
        return setupDescriptor.asObjectNode();
    }

    @Override
    protected JsonNode setup(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                             FoDScanConfigMobileDescriptor currentSetup) {
        var releaseId = releaseDescriptor.getReleaseId();

        LOG.info("Finding appropriate entitlement to use.");

        var atd = FoDReleaseAssessmentTypeHelper.getAssessmentTypeDescriptor(unirest, releaseId, FoDScanType.Mobile,
                entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), assessmentType);
        Integer assessmentTypeId = atd.getAssessmentTypeId();
        Integer entitlementIdToUse = atd.getEntitlementId();
        assessmentTypeName = atd.getName();

        validateEntitlement(currentSetup, entitlementIdToUse, releaseId, atd);
        LOG.info("Configuring release to use entitlement " + entitlementIdToUse);

        // Note: for some reason the API uses "None" for Automated audit but shows "Automated" in the UI!
        var auditPreference = auditPreferenceType.name();
        if (auditPreferenceType.name().equals("Automated")) {
            auditPreference = "None";
        }

        FoDScanConfigMobileSetupRequest setupMastScanRequest = FoDScanConfigMobileSetupRequest.builder()
                .assessmentTypeId(assessmentTypeId)
                // Note: entitlementFrequencyType is not actually used by the API, but we include it for completeness
                .entitlementFrequencyType(entitlementFrequencyTypeMixin.getEntitlementFrequencyType().name())
                .frameworkType(mobileFramework.name())
                .platformType(mobilePlatform.name())
                .auditPreferenceType(auditPreference)
                .timeZone(timezone).build();

        return FoDScanConfigMobileHelper.setupScan(unirest, releaseDescriptor, setupMastScanRequest).asJsonNode();
    }

    private void validateEntitlement(FoDScanConfigMobileDescriptor currentSetup, Integer entitlementIdToUse, String releaseId, FoDReleaseAssessmentTypeDescriptor atd) {
        // validate entitlement specified or currently in use against assessment type found
        if (entitlementId != null && entitlementId > 0) {
            // check if "entitlement id" explicitly matches what has been found
            if (!Objects.equals(entitlementIdToUse, entitlementId)) {
                throw new FcliSimpleException("Cannot find appropriate assessment type for use with entitlement: " + entitlementId + "=" + entitlementIdToUse);
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
        if (FoDReleaseAssessmentTypeHelper.validateEntitlementCanBeUsed(releaseId, atd)) {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is still valid.");
        } else {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is no longer valid.");
        }
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(getUnirestInstance());
        return ((ObjectNode)record)
            .put("scanType", getScanType())
            .put("setupType", getSetupType())
            .put("applicationName", releaseDescriptor.getApplicationName())
            .put("releaseName", releaseDescriptor.getReleaseName())
            .put("microserviceName", releaseDescriptor.getMicroserviceName());
    }
 
}
