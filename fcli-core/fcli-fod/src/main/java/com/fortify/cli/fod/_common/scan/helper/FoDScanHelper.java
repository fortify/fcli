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

import static java.util.function.Predicate.not;

import java.util.Calendar;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.dast.FoDScanDastAutomatedHelper;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.dast_scan.helper.FoDScanConfigDastAutomatedDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseAssessmentTypeHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupType;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Log LOG = LogFactory.getLog(FoDScanHelper.class);


    // max retention period (in years) of FPRs
    public static int MAX_RETENTION_PERIOD = 2;

    public static final JsonNode renameFields(JsonNode record, FoDScanType scanType) {
        var obj = (ObjectNode)new RenameFieldsTransformer(new String[] {
                "ScanId:scanId",
                "AnalysisStatusId:analysisStatusTypeId",
                "AnalysisStatusTypeValue:analysisStatusType",
                "IssueCountCritical:issueCountCritical",
                "IssueCountHigh:issueCountHigh",
                "IssueCountMedium:issueCountMedium",
                "IssueCountLow:issueCountLow"
        }).transform(record);
        if (obj.has("ScanType")) {
            obj.put("scanTypeId", obj.get("ScanType").intValue());
            obj.put("scanType", scanType.name());
            obj.remove("ScanType");
        }
        return obj;
    }
    public static final FoDScanDescriptor getScanDescriptor(UnirestInstance unirest, String releaseQualifiedScanOrId, String delimiter) {
        String[] elts = (delimiter != null) ? releaseQualifiedScanOrId.split(delimiter) : new String[]{releaseQualifiedScanOrId};
        switch (elts.length) {
            case 2:
                var pollingResult = unirest.get(FoDUrls.SCAN_POLLING_SUMMARY)
                        .routeParam("relId", elts[0])
                        .routeParam("scanId", elts[1])
                        .asObject(ObjectNode.class)
                        .getBody();
                return getDescriptor(pollingResult);
            case 1:
                var summaryResult = unirest.get(FoDUrls.SCAN + "/summary")
                        .routeParam("scanId", elts[0])
                        .asObject(ObjectNode.class)
                        .getBody();
                return getDescriptor(summaryResult);
            default:
                throw new FcliSimpleException("Scan must be specified in the format <release id>" + delimiter + "<scan id> or <scan id>");
        }
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
                throw new FcliSimpleException(ex.getMessage());
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
            throw new FcliSimpleException(
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

        var atd = FoDReleaseAssessmentTypeHelper.getAssessmentTypeDescriptor(unirest, relId, scanType, 
            entitlementFrequencyType, assessmentType);
        assessmentTypeId = atd.getAssessmentTypeId();
        entitlementIdToUse = atd.getEntitlementId();

        // validate entitlement specified or currently in use against assessment type found
        if (entitlementId > 0) {
            // check if "entitlement id" explicitly matches what has been found
            if (!Objects.equals(entitlementIdToUse, entitlementId)) {
                throw new FcliSimpleException("Cannot find appropriate assessment type for use with entitlement: " + entitlementId);
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

        // check if the entitlement ican be used
        if (FoDReleaseAssessmentTypeHelper.validateEntitlementCanBeUsed(relId, atd)) {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is still valid.");
        } else {
            LOG.info("The entitlement '" + entitlementIdToUse + "' is no longer valid.");
        }

        return FoDScanAssessmentTypeDescriptor.builder()
                .assessmentTypeId(assessmentTypeId)
                .frequencyType(String.valueOf(FoDEnums.EntitlementFrequencyType.Subscription))
                .entitlementId(entitlementIdToUse)
                .build();
    }

    public static final HttpRequest<?> addDefaultScanListParams(HttpRequest<?> request) {
        return request.queryString("orderBy", "startedDateTime")
                .queryString("orderByDirection", "DESC");
    }

    private static final FoDScanDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }

    private static final FoDScanDescriptor getEmptyDescriptor() {
        return JsonHelper.treeToValue(getObjectMapper().createObjectNode(), FoDScanDescriptor.class);
    }

    public static void cancelScan(UnirestInstance unirest, String releaseId, String scanId) {
        JsonNode cancelResponse = unirest.post(FoDUrls.RELEASE + "/scans/{scanId}/cancel-scan")
                .routeParam("relId", releaseId)
                .routeParam("scanId", scanId)
                .asObject(JsonNode.class).getBody();
        if (cancelResponse.has("success") && !cancelResponse.get("success").asBoolean()) {
            throw new IllegalStateException("Error cancelling scan " + cancelResponse.get("message").asText());
        }
    }

}
