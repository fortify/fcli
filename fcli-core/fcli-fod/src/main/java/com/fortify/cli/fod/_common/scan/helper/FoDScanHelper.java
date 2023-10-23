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
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupType;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

// TODO Class contains some fairly long methods; consider splitting methods
public class FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // max retention period (in years) of FPRs
    public static int MAX_RETENTION_PERIOD = 2;

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

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

    //

    private static final FoDScanDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }

    private static final FoDScanDescriptor getEmptyDescriptor() {
        return JsonHelper.treeToValue(getObjectMapper().createObjectNode(), FoDScanDescriptor.class);
    }

    public static final HttpRequest<?> addDefaultScanListParams(HttpRequest<?> request) {
        return request.queryString("orderBy", "startedDateTime")
                .queryString("orderByDirection", "DESC");
    }


}
