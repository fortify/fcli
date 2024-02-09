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
import com.fortify.cli.fod._common.output.cli.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod._common.scan.cli.cmd.AbstractFoDScanSetupCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanAssessmentTypeDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupBaseRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Command(name = FoDOutputHelperMixins.SetupApi.CMD_NAME) @CommandGroup("*-scan-setup")
public class FoDDastAutomatedScanSetupApiCommand extends AbstractFoDScanSetupCommand {
    @Getter @Mixin private FoDOutputHelperMixins.SetupWorkflow outputHelper;

    @Option(names={"--type"}, required = true)
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

    @Option(names={"--environment"}, defaultValue = "External")
    private FoDEnums.DynamicScanEnvironmentFacingType environmentFacingType;
    @Option(names={"--timebox"})
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

    @Override
    protected String getScanType() {
        return "DAST Automated";
    }
    @Override
    protected String getSetupType() {
        return "API";
    }

    @Override
    protected HttpRequest<?> getBaseRequest(UnirestInstance unirest, String releaseId) {
        validate();

        FoDEnums.DastAutomatedFileTypes dastFileType = apiType.getDastFileType();
        boolean requiresNetworkAuthentication = false;
        Integer fileIdToUse = fileId;

        if (uploadFileMixin != null && uploadFileMixin.getFile() != null) {
            fileIdToUse = uploadFileToUse(unirest, releaseId, FoDScanType.Dynamic, dastFileType.name());
        }
        FoDScanDastAutomatedSetupBaseRequest.NetworkAuthenticationType networkAuthenticationSettings = null;
        if (networkAuthenticationType != null) {
            requiresNetworkAuthentication = true;
            networkAuthenticationSettings = new FoDScanDastAutomatedSetupBaseRequest.NetworkAuthenticationType(networkAuthenticationType, username, password);
        }
        String timeZoneToUse = FoDScanHelper.validateTimezone(unirest, timezone);

        FoDScanAssessmentTypeDescriptor assessmentTypeDescriptor = FoDScanHelper.getEntitlementToUse(unirest, releaseId, FoDScanType.Dynamic,
                assessmentType, entitlementFrequencyTypeMixin.getEntitlementFrequencyType(), entitlementId);
        entitlementId = assessmentTypeDescriptor.getEntitlementId();

        FoDScanDastAutomatedSetupBaseRequest setupBaseRequest = FoDScanDastAutomatedSetupBaseRequest.builder()
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ?
                        environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.Internal)
                .requestFalsePositiveRemoval(requestFalsePositiveRemoval != null ? requestFalsePositiveRemoval : false)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(requiresNetworkAuthentication)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .timeBoxInHours(timebox)
                .assessmentTypeId(assessmentTypeDescriptor.getAssessmentTypeId())
                .entitlementId(entitlementId)
                .entitlementFrequencyType(FoDEnums.EntitlementFrequencyType.valueOf(assessmentTypeDescriptor.getFrequencyType()))
                .build();

        if (apiType.equals(FoDEnums.DastAutomatedApiTypes.Postman)) {
            ArrayList<Integer> collectionFileIds = new ArrayList<>(Collections.singletonList(fileIdToUse));
            return FoDScanHelper.getPostmanSetupRequest(unirest, releaseId, setupBaseRequest, collectionFileIds);
        } else if (apiType.equals(FoDEnums.DastAutomatedApiTypes.OpenApi)) {
            return FoDScanHelper.getOpenApiSetupRequest(unirest, releaseId, setupBaseRequest, fileIdToUse, apiUrl, apiKey);
        } else if (apiType.equals(FoDEnums.DastAutomatedApiTypes.GraphQL)) {
            return FoDScanHelper.getGraphQlSetupRequest(unirest, releaseId, setupBaseRequest, fileIdToUse, apiUrl, apiSchemeType, apiHost, apiServicePath);
        } else if (apiType.equals(FoDEnums.DastAutomatedApiTypes.GRPC)) {
            return FoDScanHelper.getGrpcSetupRequest(unirest, releaseId, setupBaseRequest, fileIdToUse, apiSchemeType, apiHost, apiServicePath);
        } else {
            throw new IllegalArgumentException("Unexpected DAST Automated API type: " + apiType);
        }
    }

    private void validate() {
        if (apiUrl != null && !apiUrl.isEmpty()) {
            if (!apiUrl.matches("^https://.*")) {
                throw new IllegalArgumentException("The 'apiUrl' option must include SSL with hostname.");
            }
        }
    }

}
