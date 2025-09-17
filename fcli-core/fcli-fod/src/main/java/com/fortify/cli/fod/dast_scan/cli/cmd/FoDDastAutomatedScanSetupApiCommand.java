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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.fod._common.output.cli.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.scan.helper.FoDScanAssessmentTypeDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupBaseRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.dast_scan.helper.FileUploadResult;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedDescriptor;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Collections;

@Command(name = FoDOutputHelperMixins.SetupApi.CMD_NAME)
@CommandGroup("*-scan-setup")
public class FoDDastAutomatedScanSetupApiCommand extends AbstractFoDDastAutomatedScanSetupCommand {
    private static final Log LOG = LogFactory.getLog(FoDDastAutomatedScanSetupApiCommand.class);
    @Getter @Mixin private FoDOutputHelperMixins.SetupWorkflow outputHelper;

    @Option(names = {"--type"}, required = true)
    private FoDEnums.DastAutomatedApiTypes apiType;

    @Option(names = {"--file-id"})
    private Integer fileId;

    @Option(names = {"--url", "--api-url"})
    private String apiUrl;

    @Option(names = {"--key", "--api-key"})
    private String apiKey;
    @Option(names = {"--scheme-type"})
    private FoDEnums.ApiSchemeType apiSchemeType;
    @Option(names = {"--host"})
    private String apiHost;
    @Option(names = {"--service-path"})
    private String apiServicePath;

    @Option(names = {"--environment"}, defaultValue = "External")
    private FoDEnums.DynamicScanEnvironmentFacingType environmentFacingType;
    @Option(names = {"--timebox"})
    private Integer timebox;
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
        return "API";
    }

    @Override
    protected JsonNode setup(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanConfigDastAutomatedDescriptor currentSetup) {
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

        FoDEnums.DastAutomatedFileTypes dastFileType = apiType.getDastFileType();
        FileUploadResult fileUploadResult = handleFileUpload(unirest, relId, dastFileType);

        FoDScanDastAutomatedSetupBaseRequest.NetworkAuthenticationType networkAuthenticationSettings =
                getNetworkAuthenticationSettings();

        String timeZoneToUse = getTimeZoneToUse(unirest);

        if (fodConnectNetwork != null) {
            environmentFacingType = FoDEnums.DynamicScanEnvironmentFacingType.Internal;
        }

        FoDScanAssessmentTypeDescriptor assessmentTypeDescriptor = getEntitlementToUse(unirest, relId);
        entitlementId = assessmentTypeDescriptor.getEntitlementId();

        FoDScanDastAutomatedSetupBaseRequest setupBaseRequest = buildSetupRequest(
                networkAuthenticationSettings, timeZoneToUse, assessmentTypeDescriptor
        );

        return buildResultNode(unirest, releaseDescriptor, setupBaseRequest, fileUploadResult);
    }

    private FileUploadResult handleFileUpload(UnirestInstance unirest, String relId, FoDEnums.DastAutomatedFileTypes dastFileType) {
        if (uploadFileMixin != null && uploadFileMixin.getFile() != null) {
            return uploadFileToUse(unirest, relId, FoDScanType.Dynamic, dastFileType != null ? dastFileType.name() : null);
        }
        return null;
    }

    private FoDScanDastAutomatedSetupBaseRequest.NetworkAuthenticationType getNetworkAuthenticationSettings() {
        if (networkAuthenticationType != null) {
            return new FoDScanDastAutomatedSetupBaseRequest.NetworkAuthenticationType(networkAuthenticationType, username, password);
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

    private FoDScanDastAutomatedSetupBaseRequest buildSetupRequest(
            FoDScanDastAutomatedSetupBaseRequest.NetworkAuthenticationType networkAuthenticationSettings,
            String timeZoneToUse,
            FoDScanAssessmentTypeDescriptor assessmentTypeDescriptor
    ) {
        return FoDScanDastAutomatedSetupBaseRequest.builder()
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ?
                        environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.Internal)
                .requestFalsePositiveRemoval(requestFalsePositiveRemoval != null ? requestFalsePositiveRemoval : false)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(networkAuthenticationSettings != null)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .timeBoxInHours(timebox)
                .assessmentTypeId(assessmentTypeDescriptor.getAssessmentTypeId())
                .entitlementId(entitlementId)
                .entitlementFrequencyType(FoDEnums.EntitlementFrequencyType.valueOf(assessmentTypeDescriptor.getFrequencyType()))
                .networkName(fodConnectNetwork != null ? fodConnectNetwork : "")
                .build();
    }

    private ObjectNode buildResultNode(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                       FoDScanDastAutomatedSetupBaseRequest setupBaseRequest,
                                       FileUploadResult fileUploadResult) {
        ObjectNode node;
        if (apiType.equals(FoDEnums.DastAutomatedApiTypes.Postman)) {
            node = FoDScanConfigDastAutomatedHelper.setupPostmanScan(unirest, releaseDescriptor, setupBaseRequest,
                    new ArrayList<>(Collections.singletonList(fileUploadResult != null ? fileUploadResult.getFileId() : 0))).asObjectNode();
        } else if (apiType.equals(FoDEnums.DastAutomatedApiTypes.OpenApi)) {
            node = FoDScanConfigDastAutomatedHelper.setupOpenApiScan(unirest, releaseDescriptor, setupBaseRequest,
                    fileUploadResult, apiUrl, apiKey).asObjectNode();
        } else if (apiType.equals(FoDEnums.DastAutomatedApiTypes.GraphQL)) {
            node = FoDScanConfigDastAutomatedHelper.setupGraphQlScan(unirest, releaseDescriptor, setupBaseRequest,
                    fileUploadResult, apiUrl, apiSchemeType, apiHost, apiServicePath).asObjectNode();
        } else if (apiType.equals(FoDEnums.DastAutomatedApiTypes.GRPC)) {
            node = FoDScanConfigDastAutomatedHelper.setupGrpcScan(unirest, releaseDescriptor, setupBaseRequest,
                    fileUploadResult, apiSchemeType, apiHost, apiServicePath).asObjectNode();
        } else {
            throw new FcliSimpleException("Unexpected DAST Automated API type: " + apiType);
        }
        node.put("scanType", getScanType())
                .put("setupType", getSetupType())
                .put("filename", (uploadFileMixin.getFile() != null ? uploadFileMixin.getFile().getName() : "N/A"))
                .put("entitlementId", entitlementId)
                .put("fileId", (fileUploadResult != null ? fileUploadResult.getFileId() : 0))
                .put("applicationName", releaseDescriptor.getApplicationName())
                .put("releaseName", releaseDescriptor.getReleaseName())
                .put("microserviceName", releaseDescriptor.getMicroserviceName());
        return node;
    }

    private void validate() {
        if (apiUrl != null && !apiUrl.isEmpty()) {
            if (!apiUrl.matches("^https://.*")) {
                throw new FcliSimpleException("The 'apiUrl' option must include SSL with hostname.");
            }
        }
    }

}
