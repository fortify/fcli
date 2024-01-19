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

package com.fortify.cli.fod._common.scan.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.dast.*;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupType;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import static java.util.function.Predicate.not;

public class FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Log LOG = LogFactory.getLog(FoDScanHelper.class);


    // max retention period (in years) of FPRs
    public static int MAX_RETENTION_PERIOD = 2;

    public static final FoDScanDescriptor getScanDescriptor(UnirestInstance unirest, String scanId) {
        var result = unirest.get(FoDUrls.SCAN + "/summary")
                .routeParam("scanId", scanId)
                .asObject(ObjectNode.class)
                .getBody();
        return getDescriptor(result);
    }

    public static final FoDScanDescriptor getLatestScanDescriptor(UnirestInstance unirest, String relId,
                                                                  FoDScanType scanType,
                                                                  boolean latestById) {
        String queryField = (latestById ? "scanId" : "startedDateTime");
        Optional<JsonNode> latestScan = JsonHelper.stream(
                        (ArrayNode) unirest.get(FoDUrls.RELEASE_SCANS).routeParam("relId", relId)
                                .queryString("orderBy", queryField)
                                .queryString("orderByDirection", "DESC")
                                .asObject(JsonNode.class).getBody().get("items")
                )
                .filter(n -> n.get("scanType").asText().equals(scanType.name()))
                .filter(not(n -> n.get("analysisStatusType").asText().equals("In_Progress")))
                .findFirst();
        return (latestScan.isEmpty() ? getEmptyDescriptor() : getDescriptor(latestScan.get()));
    }

    public static String validateTimezone(UnirestInstance unirest, String timezone) {
        FoDLookupDescriptor lookupDescriptor = null;
        if (timezone != null && !timezone.isEmpty()) {
            try {
                lookupDescriptor = FoDLookupHelper.getDescriptor(unirest, FoDLookupType.TimeZones, timezone, false);
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex.getMessage());
            }
            return lookupDescriptor.getValue();
        } else {
            // default to UTC
            return "UTC";
        }
    }

    public static void validateScanDate(FoDScanDescriptor scanDescriptor, int retentionPeriod) throws RuntimeException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -retentionPeriod);
        if (scanDescriptor.getCompletedDateTime() == null ||
                scanDescriptor.getCompletedDateTime().before(cal.getTime())) {
            throw new RuntimeException(
                    String.format("The last scan date was over %d years ago and results are no longer available to be downloaded.", retentionPeriod));
        }
    }

    public static FoDScanAssessmentTypeDescriptor getEntitlementToUse(UnirestInstance unirest, String relId, FoDScanType scanType,
                                                                      String assessmentType, FoDEnums.EntitlementFrequencyType entitlementFrequencyType,
                                                                      int entitlementId) {
        FoDScanConfigDastAutomatedDescriptor currentSetup = null;
        try {
            currentSetup = FoDScanDastAutomatedHelper.getSetupDescriptor(unirest, relId);
        } catch (UnexpectedHttpResponseException ex) {
            // we have no current setup;
            LOG.info("Unable to find current setup: " + ex);
        }
        Integer entitlementIdToUse = 0;
        Integer assessmentTypeId = 0;
        LOG.info("Finding/Validating entitlement to use.");

        // first find an appropriate assessment type to use
        Optional<FoDReleaseAssessmentTypeDescriptor> atd = Arrays.stream(
                        FoDReleaseAssessmentTypeHelper.getAssessmentTypes(unirest, relId, scanType, entitlementFrequencyType,
                                false, true)
                ).filter(n -> n.getName().equals(assessmentType))
                .findFirst();
        if (atd.isEmpty()) {
            throw new IllegalArgumentException("Cannot find appropriate assessment type for specified options.");
        }
        assessmentTypeId = atd.get().getAssessmentTypeId();
        entitlementIdToUse = atd.get().getEntitlementId();

        // validate entitlement specified or currently in use against assessment type found
        if (entitlementId > 0) {
            // check if "entitlement id" explicitly matches what has been found
            if (!Objects.equals(entitlementIdToUse, entitlementId)) {
                throw new IllegalArgumentException("Cannot appropriate assessment type for use with entitlement: " + entitlementId);
            }
            LOG.info("The 'entitlement-id' specified by user '" + entitlementId + "' is valid.");
        } else {
            if (currentSetup != null && (currentSetup.getEntitlementId() != null && currentSetup.getEntitlementId() > 0)) {
                // check if "entitlement id" is already configured
                if (!Objects.equals(entitlementIdToUse, currentSetup.getEntitlementId())) {
                    LOG.warn("Changing current release entitlement from '" + currentSetup.getEntitlementId() + "'.");
                }
            }
        }
        LOG.info("Configuring release to use entitlement '" + entitlementIdToUse + "'.");

        // check if the entitlement is still valid
        FoDReleaseAssessmentTypeHelper.validateEntitlement(relId, atd.get());
        LOG.info("The entitlement '" + entitlementIdToUse + "' is still valid.");

        return FoDScanAssessmentTypeDescriptor.builder()
                .assessmentTypeId(assessmentTypeId)
                .frequencyType(String.valueOf(FoDEnums.EntitlementFrequencyType.Subscription))
                .entitlementId(entitlementIdToUse)
                .build();
    }

    private static final FoDScanDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }

    //

    private static final FoDScanDescriptor getEmptyDescriptor() {
        return JsonHelper.treeToValue(getObjectMapper().createObjectNode(), FoDScanDescriptor.class);
    }

    public static final HttpRequest<?> addDefaultScanListParams(HttpRequest<?> request) {
        return request.queryString("orderBy", "startedDateTime")
                .queryString("orderByDirection", "DESC");
    }

    public static HttpRequest<?> getPostmanSetupRequest(UnirestInstance unirest, String releaseId, Integer timeBox, String timeZoneToUse, FoDEnums.DynamicScanEnvironmentFacingType environmentFacingType,
                                                        Boolean requiresNetworkAuthentication,
                                                        FoDScanDastAutomatedSetupApiRequest.NetworkAuthenticationType networkAuthenticationSettings,
                                                        Integer assessmentTypeId, Integer entitlementId, FoDEnums.EntitlementFrequencyType entitlementFrequencyType,
                                                        ArrayList<Integer> collectionFileIds) {
        FoDScanDastAutomatedSetupPostmanRequest setupRequest = FoDScanDastAutomatedSetupPostmanRequest.builder()
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ?
                        environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.Internal)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(requiresNetworkAuthentication)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .timeBoxInHours(timeBox)
                .assessmentTypeId(assessmentTypeId)
                .entitlementId(entitlementId)
                .entitlementFrequencyType(entitlementFrequencyType)
                .collectionFileIds(collectionFileIds)
                .build();
        return unirest.put(FoDUrls.DAST_AUTOMATED_SCANS + "/postman-scan-setup")
                .routeParam("relId", releaseId)
                .body(setupRequest);
    }

    @SneakyThrows
    public static HttpRequest<?> getOpenApiSetupRequest(UnirestInstance unirest, String releaseId, Integer timebox, String timeZoneToUse, FoDEnums.DynamicScanEnvironmentFacingType environmentFacingType,
                                                        Boolean requiresNetworkAuthentication,
                                                        FoDScanDastAutomatedSetupApiRequest.NetworkAuthenticationType networkAuthenticationSettings,
                                                        Integer assessmentTypeId, Integer entitlementId, FoDEnums.EntitlementFrequencyType entitlementFrequencyType,
                                                        Integer fileId, String apiUrl, String apiKey) {
        boolean isUrl = (apiUrl != null && !apiUrl.isEmpty());
        int fileIdToUse = (fileId != null ? fileId : 0);
        FoDScanDastAutomatedSetupOpenApiRequest setupRequest = FoDScanDastAutomatedSetupOpenApiRequest.builder()
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ?
                        environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.Internal)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(requiresNetworkAuthentication)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .timeBoxInHours(timebox)
                .assessmentTypeId(assessmentTypeId)
                .entitlementId(entitlementId)
                .entitlementFrequencyType(entitlementFrequencyType)
                .sourceType(isUrl ? "Url" : "FileId")
                .sourceUrn(isUrl ? apiUrl : String.valueOf(fileIdToUse))
                .apiKey(apiKey)
                .build();

        return unirest.put(FoDUrls.DAST_AUTOMATED_SCANS + "/openapi-scan-setup")
                .routeParam("relId", releaseId)
                .body(setupRequest);
    }

    public static HttpRequest<?> getGraphQlSetupRequest(UnirestInstance unirest, String releaseId, Integer timebox, String timeZoneToUse, FoDEnums.DynamicScanEnvironmentFacingType environmentFacingType,
                                                        Boolean requiresNetworkAuthentication,
                                                        FoDScanDastAutomatedSetupApiRequest.NetworkAuthenticationType networkAuthenticationSettings,
                                                        Integer assessmentTypeId, Integer entitlementId, FoDEnums.EntitlementFrequencyType entitlementFrequencyType,
                                                        Integer fileId, String apiUrl, FoDEnums.ApiSchemeType schemeType, String host, String servicePath) {
        boolean isUrl = (apiUrl != null && !apiUrl.isEmpty());
        int fileIdToUse = (fileId != null ? fileId : 0);
        FoDScanDastAutomatedSetupGraphQlRequest setupRequest = FoDScanDastAutomatedSetupGraphQlRequest.builder()
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ?
                        environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.Internal)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(requiresNetworkAuthentication)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .timeBoxInHours(timebox)
                .assessmentTypeId(assessmentTypeId)
                .entitlementId(entitlementId)
                .entitlementFrequencyType(entitlementFrequencyType)
                .sourceType(isUrl ? "Url" : "FileId")
                .sourceUrn(isUrl ? apiUrl : String.valueOf(fileIdToUse))
                .schemeType(schemeType)
                .host(host)
                .servicePath(servicePath)
                .build();

        return unirest.put(FoDUrls.DAST_AUTOMATED_SCANS + "/graphql-scan-setup")
                .routeParam("relId", releaseId)
                .body(setupRequest);
    }

    public static HttpRequest<?> getGrpcSetupRequest(UnirestInstance unirest, String releaseId, Integer timebox, String timeZoneToUse, FoDEnums.DynamicScanEnvironmentFacingType environmentFacingType,
                                                        Boolean requiresNetworkAuthentication,
                                                        FoDScanDastAutomatedSetupApiRequest.NetworkAuthenticationType networkAuthenticationSettings,
                                                        Integer assessmentTypeId, Integer entitlementId, FoDEnums.EntitlementFrequencyType entitlementFrequencyType,
                                                        Integer fileId, FoDEnums.ApiSchemeType schemeType, String host, String servicePath) {
        FoDScanDastAutomatedSetupGrpcRequest setupRequest = FoDScanDastAutomatedSetupGrpcRequest.builder()
                .dynamicScanEnvironmentFacingType(environmentFacingType != null ?
                        environmentFacingType :
                        FoDEnums.DynamicScanEnvironmentFacingType.Internal)
                .timeZone(timeZoneToUse)
                .requiresNetworkAuthentication(requiresNetworkAuthentication)
                .networkAuthenticationSettings(networkAuthenticationSettings)
                .timeBoxInHours(timebox)
                .assessmentTypeId(assessmentTypeId)
                .entitlementId(entitlementId)
                .entitlementFrequencyType(entitlementFrequencyType)
                .fileId(fileId)
                .schemeType(schemeType)
                .host(host)
                .servicePath(servicePath)
                .build();

        return unirest.put(FoDUrls.DAST_AUTOMATED_SCANS + "/grpc-scan-setup")
                .routeParam("relId", releaseId)
                .body(setupRequest);
    }


}
