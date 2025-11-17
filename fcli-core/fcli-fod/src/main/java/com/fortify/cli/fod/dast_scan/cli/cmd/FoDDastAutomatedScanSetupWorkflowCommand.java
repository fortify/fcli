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
package com.fortify.cli.fod.dast_scan.cli.cmd;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod._common.output.cli.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanAssessmentTypeDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupWorkflowRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.dast_scan.helper.FileUploadResult;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedDescriptor;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = FoDOutputHelperMixins.SetupWorkflow.CMD_NAME) @CommandGroup("*-scan-setup")
public class FoDDastAutomatedScanSetupWorkflowCommand extends AbstractFoDDastAutomatedScanSetupCommand {
    private static final Logger LOG = LoggerFactory.getLogger(FoDDastAutomatedScanSetupWorkflowCommand.class);
    @Getter @Mixin private FoDOutputHelperMixins.SetupWorkflow outputHelper;
    private final static FoDEnums.DastAutomatedFileTypes dastFileType = FoDEnums.DastAutomatedFileTypes.WorkflowDrivenMacro;

    //@Option(names={"--hosts", "--allowed-hosts"}, split=",")
    //private ArrayList<String> allowedHosts;
    @Option(names = {"--file-id"})
    private Integer workflowMacroFileId;
    @Option(names={"--policy"}, required = true, defaultValue = "Standard")
    private String scanPolicy;
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
    @Option(names = {"--vpn"})
    private String fodConnectNetwork;

    @Override
    protected String getSetupType() {
        return "Workflow";
    }

    @Override
    protected JsonNode setup(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                            FoDScanConfigDastAutomatedDescriptor currentSetup) {
        var relId = releaseDescriptor.getReleaseId();

        validate();

        LOG.info("Finding appropriate entitlement to use.");
        var atd = FoDReleaseAssessmentTypeHelper.getAssessmentTypeDescriptor(unirest, relId, FoDScanType.Dynamic,
                entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), assessmentType);
        var assessmentTypeId = atd.getAssessmentTypeId();
        var entitlementIdToUse = atd.getEntitlementId();
        assessmentTypeName = atd.getName();
        if (currentSetup != null) validateEntitlement(currentSetup, entitlementIdToUse, relId, atd);
        LOG.info("Configuring release to use entitlement " + entitlementIdToUse);

        FileUploadResult fileUploadResult = handleFileUpload(unirest, relId);
        ArrayList<FoDScanDastAutomatedSetupWorkflowRequest.WorkflowDrivenMacro> workflowDrivenMacros =
                buildWorkflowDrivenMacros(fileUploadResult);

        FoDScanDastAutomatedSetupWorkflowRequest.NetworkAuthenticationType networkAuthenticationSettings =
                getNetworkAuthenticationSettings();

        String timeZoneToUse = getTimeZoneToUse(unirest);

        if (fodConnectNetwork != null) {
            environmentFacingType = FoDEnums.DynamicScanEnvironmentFacingType.Internal;
        }

        FoDScanAssessmentTypeDescriptor assessmentTypeDescriptor = getEntitlementToUse(unirest, relId);

        entitlementId = assessmentTypeDescriptor.getEntitlementId();

        FoDScanDastAutomatedSetupWorkflowRequest setupRequest = buildSetupRequest(
                workflowDrivenMacros, networkAuthenticationSettings, timeZoneToUse, assessmentTypeDescriptor);

        return buildResultNode(unirest, releaseDescriptor, setupRequest, fileUploadResult, workflowDrivenMacros);
    }

    private FileUploadResult handleFileUpload(UnirestInstance unirest, String relId) {
        if (uploadFileMixin != null && uploadFileMixin.getFile() != null) {
            return uploadFileToUse(unirest, relId, FoDScanType.Dynamic, dastFileType.name());
        }
        return new FileUploadResult(workflowMacroFileId != null ? workflowMacroFileId : 0, null);
    }

    private ArrayList<FoDScanDastAutomatedSetupWorkflowRequest.WorkflowDrivenMacro> buildWorkflowDrivenMacros(FileUploadResult fileUploadResult) {
        ArrayList<FoDScanDastAutomatedSetupWorkflowRequest.WorkflowDrivenMacro> workflowDrivenMacros = new ArrayList<>();
        workflowDrivenMacros.add(new FoDScanDastAutomatedSetupWorkflowRequest.WorkflowDrivenMacro(
                fileUploadResult.getFileId(), fileUploadResult.getHosts()));
        return workflowDrivenMacros;
    }

    private FoDScanDastAutomatedSetupWorkflowRequest.NetworkAuthenticationType getNetworkAuthenticationSettings() {
        if (networkAuthenticationType != null) {
            return new FoDScanDastAutomatedSetupWorkflowRequest.NetworkAuthenticationType(
                    networkAuthenticationType, username, password);
        }
        return null;
    }

    private String getTimeZoneToUse(UnirestInstance unirest) {
        return FoDScanHelper.validateTimezone(unirest, timezone);
    }

    private FoDScanAssessmentTypeDescriptor getEntitlementToUse(UnirestInstance unirest, String relId) {
        return FoDScanHelper.getEntitlementToUse(
                unirest, relId, FoDScanType.Dynamic,
                assessmentType, entitlementFrequencyTypeMixin.getEntitlementFrequencyType(),
                entitlementId != null && entitlementId > 0 ? entitlementId : 0);
    }

    private FoDScanDastAutomatedSetupWorkflowRequest buildSetupRequest(
            ArrayList<FoDScanDastAutomatedSetupWorkflowRequest.WorkflowDrivenMacro> workflowDrivenMacros,
            FoDScanDastAutomatedSetupWorkflowRequest.NetworkAuthenticationType networkAuthenticationSettings,
            String timeZoneToUse,
            FoDScanAssessmentTypeDescriptor assessmentTypeDescriptor) {

        boolean requiresNetworkAuthentication = networkAuthenticationSettings != null;

        return FoDScanDastAutomatedSetupWorkflowRequest.builder()
                .workflowDrivenMacro(workflowDrivenMacros)
                .policy(scanPolicy)
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ? environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.Internal)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(requiresNetworkAuthentication)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .assessmentTypeId(assessmentTypeDescriptor.getAssessmentTypeId())
                .entitlementId(entitlementId)
                .entitlementFrequencyType(FoDEnums.EntitlementFrequencyType.valueOf(assessmentTypeDescriptor.getFrequencyType()))
                .requestFalsePositiveRemoval(requestFalsePositiveRemoval != null ? requestFalsePositiveRemoval : false)
                .networkName(fodConnectNetwork != null ? fodConnectNetwork : "")
                .build();
    }

    private ObjectNode buildResultNode(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                    FoDScanDastAutomatedSetupWorkflowRequest setupRequest,
                                    FileUploadResult fileUploadResult,
                                    ArrayList<FoDScanDastAutomatedSetupWorkflowRequest.WorkflowDrivenMacro> workflowDrivenMacros) {
        int fileIdToUse = (workflowMacroFileId != null ? workflowMacroFileId : 0);
        ObjectNode node = FoDScanConfigDastAutomatedHelper.setupScan(unirest, releaseDescriptor, setupRequest,
                "/workflow-scan-setup").asObjectNode();
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

    private void validate() {
        // check we have a valid workflow file
        if (uploadFileMixin == null || uploadFileMixin.getFile() == null) {
            throw new FcliSimpleException("A valid workflow macro file needs to be provided.");
        }
        /*else {
            if (allowedHosts == null || allowedHosts.isEmpty()) {
                throw new FcliSimpleException("Please specify at least one '--allowed-hosts'.");
            }
        }
        // check allowed hosts is valid
        if (allowedHosts != null && !allowedHosts.isEmpty()) {
            allowedHosts.forEach((h) -> {
                if (h.matches("^https?://.*")) {
                    throw new FcliSimpleException("The 'allowedHosts' options should not include 'http://' or 'https://'.");
                }
            });
        }*/
    }

}
