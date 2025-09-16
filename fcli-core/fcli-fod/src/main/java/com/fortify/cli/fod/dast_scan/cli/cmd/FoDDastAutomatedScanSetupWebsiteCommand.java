/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.fod.dast_scan.cli.cmd;

import java.util.ArrayList;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.fod.dast_scan.helper.FileUploadResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.fod._common.output.cli.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanAssessmentTypeDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupWebsiteRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedDescriptor;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.SetupWebsite.CMD_NAME) @CommandGroup("*-scan-setup")
public class FoDDastAutomatedScanSetupWebsiteCommand extends AbstractFoDDastAutomatedScanSetupCommand {
    private static final Log LOG = LogFactory.getLog(FoDDastAutomatedScanSetupWebsiteCommand.class);
    @Getter @Mixin private FoDOutputHelperMixins.SetupWebsite outputHelper;
    private final static FoDEnums.DastAutomatedFileTypes dastFileType = FoDEnums.DastAutomatedFileTypes.LoginMacro;

    @Option(names = {"--url", "--site-url"}, required = true)
    private String siteUrl;
    @Option(names = {"--redundant-page-detection"})
    private Boolean redundantPageProtection;
    @Option(names = {"--file-id"})
    private Integer loginMacroFileId;
    @Option(names={"-e", "--exclusions"}, split=",")
    private Set<String> exclusions;
    @Option(names={"--restrict"})
    private Boolean restrictToDirectoryAndSubdirectories;
    @Option(names={"--policy"}, required = true, defaultValue = "Standard")
    private String scanPolicy;
    @Option(names={"--timebox"})
    private Integer timebox;
    @Option(names={"--environment"}, defaultValue = "External")
    private FoDEnums.DynamicScanEnvironmentFacingType environmentFacingType;
    @Option(names = {"--timezone"})
    private String timezone;
    @Option(names = {"--network-auth-type"})
    private FoDEnums.DynamicScanNetworkAuthenticationType networkAuthenticationType;
    @Option(names = {"-u", "--network-username"})
    private String username;
    @Option(names = {"-p", "--network-password"})
    private String password;
    @Option(names = {"--false-positive-removal"})
    private Boolean requestFalsePositiveRemoval;
    @Option(names = {"--create-login-macro"})
    private Boolean createLoginMacro;
    @Option(names = {"--macro-primary-username"})
    private String macroPrimaryUsername;
    @Option(names = {"--macro-primary-password"})
    private String macroPrimaryPassword;
    @Option(names = {"--macro-secondary-username"})
    private String macroSecondaryUsername;
    @Option(names = {"--macro-secondary-password"})
    private String macroSecondaryPassword;
    @Option(names = {"--vpn"})
    private String fodConnectNetwork;

    @Override
    protected String getSetupType() {
        return "Website";
    }

    @Override
    protected JsonNode setup(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                             FoDScanConfigDastAutomatedDescriptor currentSetup) {
        var relId = releaseDescriptor.getReleaseId();

        LOG.info("Finding appropriate entitlement to use.");
        var atd = FoDReleaseAssessmentTypeHelper.getAssessmentTypeDescriptor(unirest, relId, FoDScanType.Dynamic,
                entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), assessmentType);
        var assessmentTypeId = atd.getAssessmentTypeId();
        var entitlementIdToUse = atd.getEntitlementId();
        assessmentTypeName = atd.getName();
        if (currentSetup != null) validateEntitlement(currentSetup, entitlementIdToUse, relId, atd);
        LOG.info("Configuring release to use entitlement " + entitlementIdToUse);

        boolean requiresSiteAuthentication = false;
        boolean requiresNetworkAuthentication = false;
        boolean requiresLoginMacroCreation = false;

        int fileIdToUse = getLoginMacroFileId();
        FileUploadResult fileUploadResult = handleFileUpload(unirest, relId);

        if (fileIdToUse > 0 || fileUploadResult != null) {
            requiresSiteAuthentication = true;
        }

        FoDScanDastAutomatedSetupWebsiteRequest.NetworkAuthenticationType networkAuthenticationSettings =
                getNetworkAuthenticationSettings();

        if (networkAuthenticationSettings != null) {
            requiresNetworkAuthentication = true;
        }

        ArrayList<FoDScanDastAutomatedSetupWebsiteRequest.Exclusion> exclusionsList = buildExclusionsList();

        String timeZoneToUse = getTimeZoneToUse(unirest);

        FoDScanDastAutomatedSetupWebsiteRequest.LoginMacroFileCreationType loginMacroFileCreationSettings =
                getLoginMacroFileCreationSettings();

        if (createLoginMacro != null) {
            requiresSiteAuthentication = true;
            requiresLoginMacroCreation = createLoginMacro;
        }

        if (fodConnectNetwork != null) {
            environmentFacingType = FoDEnums.DynamicScanEnvironmentFacingType.Internal;
        }

        FoDScanAssessmentTypeDescriptor assessmentTypeDescriptor = getEntitlementToUse(unirest, relId);
        entitlementId = assessmentTypeDescriptor.getEntitlementId();

        FoDScanDastAutomatedSetupWebsiteRequest setupRequest = buildSetupRequest(
                fileIdToUse, fileUploadResult, requiresSiteAuthentication, exclusionsList,
                requiresNetworkAuthentication, networkAuthenticationSettings, timeZoneToUse,
                requiresLoginMacroCreation, loginMacroFileCreationSettings, assessmentTypeDescriptor
        );

        return buildResultNode(unirest, releaseDescriptor, setupRequest, fileIdToUse, fileUploadResult);
    }

    private int getLoginMacroFileId() {
        return (loginMacroFileId != null ? loginMacroFileId : 0);
    }

    private FileUploadResult handleFileUpload(UnirestInstance unirest, String relId) {
        if (uploadFileMixin != null && uploadFileMixin.getFile() != null) {
            return uploadFileToUse(unirest, relId, FoDScanType.Dynamic, dastFileType.name());
        }
        return null;
    }

    private FoDScanDastAutomatedSetupWebsiteRequest.NetworkAuthenticationType getNetworkAuthenticationSettings() {
        if (networkAuthenticationType != null) {
            return new FoDScanDastAutomatedSetupWebsiteRequest.NetworkAuthenticationType(
                    networkAuthenticationType, username, password);
        }
        return null;
    }

    private ArrayList<FoDScanDastAutomatedSetupWebsiteRequest.Exclusion> buildExclusionsList() {
        ArrayList<FoDScanDastAutomatedSetupWebsiteRequest.Exclusion> exclusionsList = new ArrayList<>();
        if (exclusions != null && !exclusions.isEmpty()) {
            for (String s : exclusions) {
                exclusionsList.add(new FoDScanDastAutomatedSetupWebsiteRequest.Exclusion(s));
            }
        }
        return exclusionsList;
    }

    private String getTimeZoneToUse(UnirestInstance unirest) {
        return FoDScanHelper.validateTimezone(unirest, timezone);
    }

    private FoDScanDastAutomatedSetupWebsiteRequest.LoginMacroFileCreationType getLoginMacroFileCreationSettings() {
        if (createLoginMacro != null) {
            return new FoDScanDastAutomatedSetupWebsiteRequest.LoginMacroFileCreationType(
                    macroPrimaryUsername, macroPrimaryPassword, macroSecondaryUsername, macroSecondaryPassword);
        }
        return null;
    }

    private FoDScanAssessmentTypeDescriptor getEntitlementToUse(UnirestInstance unirest, String relId) {
        return FoDScanHelper.getEntitlementToUse(
                unirest, relId, FoDScanType.Dynamic,
                assessmentType, entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                entitlementId != null && entitlementId > 0 ? entitlementId : 0);
    }

    private FoDScanDastAutomatedSetupWebsiteRequest buildSetupRequest(
            int fileIdToUse,
            FileUploadResult fileUploadResult,
            boolean requiresSiteAuthentication,
            ArrayList<FoDScanDastAutomatedSetupWebsiteRequest.Exclusion> exclusionsList,
            boolean requiresNetworkAuthentication,
            FoDScanDastAutomatedSetupWebsiteRequest.NetworkAuthenticationType networkAuthenticationSettings,
            String timeZoneToUse,
            boolean requiresLoginMacroCreation,
            FoDScanDastAutomatedSetupWebsiteRequest.LoginMacroFileCreationType loginMacroFileCreationSettings,
            FoDScanAssessmentTypeDescriptor assessmentTypeDescriptor
    ) {
        return FoDScanDastAutomatedSetupWebsiteRequest.builder()
                .dynamicSiteUrl(siteUrl)
                .enableRedundantPageDetection(redundantPageProtection != null ? redundantPageProtection : false)
                .requiresSiteAuthentication(requiresSiteAuthentication)
                .loginMacroFileId(fileIdToUse != 0 ? fileIdToUse : (fileUploadResult != null ? fileUploadResult.getFileId() : 0))
                .exclusionsList(exclusionsList)
                .restrictToDirectoryAndSubdirectories(restrictToDirectoryAndSubdirectories != null ? restrictToDirectoryAndSubdirectories : false)
                .policy(scanPolicy)
                .timeBoxInHours(timebox)
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ? environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.External)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(requiresNetworkAuthentication)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .assessmentTypeId(assessmentTypeDescriptor.getAssessmentTypeId())
                .entitlementId(entitlementId)
                .entitlementFrequencyType(FoDEnums.EntitlementFrequencyType.valueOf(assessmentTypeDescriptor.getFrequencyType()))
                .requestFalsePositiveRemoval(requestFalsePositiveRemoval != null ? requestFalsePositiveRemoval : false)
                .requestLoginMacroFileCreation(requiresLoginMacroCreation)
                .loginMacroFileCreationDetails(loginMacroFileCreationSettings)
                .networkName(fodConnectNetwork != null ? fodConnectNetwork : "")
                .build();
    }

    private ObjectNode buildResultNode(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                       FoDScanDastAutomatedSetupWebsiteRequest setupRequest,
                                       int fileIdToUse,
                                       FileUploadResult fileUploadResult) {
        ObjectNode node = FoDScanConfigDastAutomatedHelper.setupScan(unirest, releaseDescriptor, setupRequest,
                "/website-scan-setup").asObjectNode();
        node.put("scanType", getScanType())
                .put("setupType", getSetupType())
                .put("filename", (uploadFileMixin.getFile() != null ? uploadFileMixin.getFile().getName() : "N/A"))
                .put("entitlementId", entitlementId)
                .put("fileId", (fileIdToUse != 0 ? fileIdToUse : (fileUploadResult != null ? fileUploadResult.getFileId() : 0)))
                .put("applicationName", releaseDescriptor.getApplicationName())
                .put("releaseName", releaseDescriptor.getReleaseName())
                .put("microserviceName", releaseDescriptor.getMicroserviceName());
        return node;
    }
}
