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

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanSetupCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanAssessmentTypeDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupWorkflowRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.ArrayList;

@Command(name = FoDOutputHelperMixins.SetupWorkflow.CMD_NAME) @CommandGroup("*-scan-setup")
public class FoDDastAutomatedScanSetupWorkflowCommand extends AbstractFoDScanSetupCommand {
    @Getter @Mixin private FoDOutputHelperMixins.SetupWorkflow outputHelper;

    private final static FoDEnums.DastAutomatedFileTypes dastFileType = FoDEnums.DastAutomatedFileTypes.WorkflowDrivenMacro;

    @Option(names={"--hosts", "--allowed-hosts"}, split=",")
    private ArrayList<String> allowedHosts;
    @Option(names = {"--redundant-page-detection"})
    private Boolean redundantPageProtection;
    @Option(names = {"--file-id"})
    private Integer workflowMacroFileId;
    @Option(names={"--policy"}, required = true)
    private FoDEnums.DastAutomatedScanPolicies scanPolicy;
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

    @Override
    protected String getScanType() {
        return "DAST Automated";
    }
    @Override
    protected String getSetupType() {
        return "Workflow";
    }

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
        validate();

        boolean requiresNetworkAuthentication = false;
        Integer fileIdToUse = workflowMacroFileId;
        if (uploadFileMixin != null && uploadFileMixin.getFile() != null) {
            fileIdToUse = uploadFileToUse(unirest, releaseId, FoDScanType.Dynamic, dastFileType.name());
        }
        ArrayList<FoDScanDastAutomatedSetupWorkflowRequest.WorkflowDrivenMacro> workflowDrivenMacros = new ArrayList<>();
        FoDScanDastAutomatedSetupWorkflowRequest.WorkflowDrivenMacro workflowDrivenMacro =
                new FoDScanDastAutomatedSetupWorkflowRequest.WorkflowDrivenMacro(fileIdToUse, allowedHosts);
        workflowDrivenMacros.add(workflowDrivenMacro);
        FoDScanDastAutomatedSetupWorkflowRequest.NetworkAuthenticationType networkAuthenticationSettings = null;
        if (networkAuthenticationType != null) {
            requiresNetworkAuthentication = true;
            networkAuthenticationSettings = new FoDScanDastAutomatedSetupWorkflowRequest.NetworkAuthenticationType(networkAuthenticationType, username, password);
        }
        String timeZoneToUse = FoDScanHelper.validateTimezone(unirest, timezone);

        FoDScanAssessmentTypeDescriptor assessmentTypeDescriptor = FoDScanHelper.getEntitlementToUse(unirest, releaseId, FoDScanType.Dynamic,
                assessmentType, entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), entitlementId);
        entitlementId = assessmentTypeDescriptor.getEntitlementId();
        FoDScanDastAutomatedSetupWorkflowRequest setupRequest = FoDScanDastAutomatedSetupWorkflowRequest.builder()
                .enableRedundantPageDetection(redundantPageProtection != null ? redundantPageProtection : false)
                .workflowDrivenMacro(workflowDrivenMacros)
                .policy(scanPolicy.name())
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ?
                        environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.Internal)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(requiresNetworkAuthentication)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .assessmentTypeId(assessmentTypeDescriptor.getAssessmentTypeId())
                .entitlementId(entitlementId)
                .entitlementFrequencyType(FoDEnums.EntitlementFrequencyType.valueOf(assessmentTypeDescriptor.getFrequencyType()))
                .build();

        return unirest.put(FoDUrls.DAST_AUTOMATED_SCANS + "/workflow-scan-setup")
                .routeParam("relId", releaseId)
                .body(setupRequest);
    }

    private void validate() {
        // check allowed hosts is valid
        if (!allowedHosts.isEmpty()) {
            allowedHosts.forEach((h) -> {
                if (h.matches("^https?://.*")) {
                    throw new IllegalArgumentException("The 'allowedHosts' options should not include 'http://' or 'https://'.");
                }
            });
        }
    }

}
