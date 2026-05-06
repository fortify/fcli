/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.fod.dast_scan.helper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupBaseRequest;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupGraphQlRequest;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupGrpcRequest;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupOpenApiRequest;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedSetupPostmanRequest;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;

public class FoDScanConfigDastAutomatedHelper {
    private static final Pattern LOCKED_SETUP_FIELD_PATTERN = Pattern.compile(
            "cannot override current scan settings:\\s*([^\"\\n-]+)",
            Pattern.CASE_INSENSITIVE);

    public static FoDScanConfigDastAutomatedDescriptor getSetupDescriptor(UnirestInstance unirest, String releaseId) {
        var body = unirest.get(FoDUrls.DAST_AUTOMATED_SCANS + "/scan-setup")
                .routeParam("relId", releaseId)
                .asObject(ObjectNode.class)
                .getBody();
        return JsonHelper.treeToValue(body, FoDScanConfigDastAutomatedDescriptor.class);
    }

    public static <T> FoDScanConfigDastAutomatedDescriptor setupScan(UnirestInstance unirest,
                                                                    FoDReleaseDescriptor releaseDescriptor,
                                                                    T setupDastAutomatedScanRequest, String ep) {
        var releaseId = releaseDescriptor.getReleaseId();
        try {
            unirest.put(FoDUrls.DAST_AUTOMATED_SCANS + ep)
                    .routeParam("relId", releaseId)
                    .body(setupDastAutomatedScanRequest)
                    .asString().getBody();
        } catch (UnexpectedHttpResponseException e) {
            throw toSetupException(e, releaseId);
        } catch (UnirestException e) {
            throw new FcliSimpleException(
                    String.format("Error configuring DAST Automated scan setup for release id '%s'", releaseId),
                    e);
        }
        return getSetupDescriptor(unirest, releaseId);
    }

    private static RuntimeException toSetupException(UnexpectedHttpResponseException e, String releaseId) {
        if (e.getStatus() == 400 && e.getMessage().toLowerCase().contains("cannot override current scan settings")) {
            var lockedSettings = extractLockedSettings(e.getMessage());
            var lockedSettingsSuffix = lockedSettings.isBlank() ? "" : " Locked settings include: " + lockedSettings;
            return new FcliSimpleException(
                    "Cannot update DAST Automated scan setup for release id '%s' because scan settings are locked after scans have started.%s",
                    releaseId, lockedSettingsSuffix);
        }
        return new FcliSimpleException("Error configuring DAST Automated scan setup for release id '%s': %s", releaseId, e.getMessage());
    }

    private static String extractLockedSettings(String message) {
        Set<String> lockedSettings = new LinkedHashSet<>();
        Matcher matcher = LOCKED_SETUP_FIELD_PATTERN.matcher(message);
        while (matcher.find()) {
            lockedSettings.add(matcher.group(1).trim());
        }
        return String.join(", ", lockedSettings);
    }

    @SneakyThrows
    public static <T> FoDScanConfigDastAutomatedDescriptor setupPostmanScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                            FoDScanDastAutomatedSetupBaseRequest base,
                                            ArrayList<Integer> collectionFileIds) {
        FoDScanDastAutomatedSetupPostmanRequest setupRequest = FoDScanDastAutomatedSetupPostmanRequest.builder()
                .collectionFileIds(collectionFileIds)
                .build();
        BeanUtils.copyProperties(base, setupRequest);
        return FoDScanConfigDastAutomatedHelper.setupScan(unirest, releaseDescriptor, setupRequest,
                "/postman-scan-setup");
    }

    @SneakyThrows
    public static <T> FoDScanConfigDastAutomatedDescriptor setupOpenApiScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                            FoDScanDastAutomatedSetupBaseRequest base, FileUploadResult fileUploadResult,
                                            String apiUrl, String apiKey) {
        boolean isUrl = (apiUrl != null && !apiUrl.isEmpty());
        int fileIdToUse = (fileUploadResult != null ? fileUploadResult.getFileId() : 0);
        FoDScanDastAutomatedSetupOpenApiRequest setupRequest = FoDScanDastAutomatedSetupOpenApiRequest.builder()
                .sourceType(isUrl ? "Url" : "FileId")
                .sourceUrn(isUrl ? apiUrl : String.valueOf(fileIdToUse))
                .apiKey(apiKey)
                .build();
        BeanUtils.copyProperties(base, setupRequest);
        return FoDScanConfigDastAutomatedHelper.setupScan(unirest, releaseDescriptor, setupRequest,
                "/openapi-scan-setup");
    }

    @SneakyThrows
    public static <T> FoDScanConfigDastAutomatedDescriptor setupGraphQlScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                            FoDScanDastAutomatedSetupBaseRequest base, FileUploadResult fileUploadResult,
                                            String apiUrl, FoDEnums.ApiSchemeType schemeType, String host,
                                            String servicePath) {
        boolean isUrl = (apiUrl != null && !apiUrl.isEmpty());
        int fileIdToUse = (fileUploadResult != null ? fileUploadResult.getFileId() : 0);
        FoDScanDastAutomatedSetupGraphQlRequest setupRequest = FoDScanDastAutomatedSetupGraphQlRequest.builder()
                .sourceType(isUrl ? "Url" : "FileId")
                .sourceUrn(isUrl ? apiUrl : String.valueOf(fileIdToUse))
                .schemeType(schemeType)
                .host(host)
                .servicePath(servicePath)
                .build();
        BeanUtils.copyProperties(base, setupRequest);
        return FoDScanConfigDastAutomatedHelper.setupScan(unirest, releaseDescriptor, setupRequest,
                "/graphql-scan-setup");
    }

    @SneakyThrows
    public static <T> FoDScanConfigDastAutomatedDescriptor setupGrpcScan(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor,
                                        FoDScanDastAutomatedSetupBaseRequest base, FileUploadResult fileUploadResult,
                                        FoDEnums.ApiSchemeType schemeType, String host, String servicePath) {
        int fileIdToUse = (fileUploadResult != null ? fileUploadResult.getFileId() : 0);
        FoDScanDastAutomatedSetupGrpcRequest setupRequest = FoDScanDastAutomatedSetupGrpcRequest.builder()
                .fileId(fileIdToUse)
                .schemeType(schemeType)
                .host(host)
                .servicePath(servicePath)
                .build();
        BeanUtils.copyProperties(base, setupRequest);
        return FoDScanConfigDastAutomatedHelper.setupScan(unirest, releaseDescriptor, setupRequest,
                "/grpc-scan-setup");
    }
}
