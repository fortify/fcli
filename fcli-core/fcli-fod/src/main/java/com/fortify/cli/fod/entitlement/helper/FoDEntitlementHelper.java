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

package com.fortify.cli.fod.entitlement.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.scan.helper.FoDAssessmentType;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public class FoDEntitlementHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public final static void validateEntitlement(UnirestInstance unirest, IProgressWriterI18n progressWriter, Integer entitlementId) throws FoDInvalidEntitlementException {
        if (entitlementId <= 0) {
            throw new FoDInvalidEntitlementException("Entitlement id should be a positive value.");
        }
        // retrieve the entitlement specified
        FoDEntitlementDescriptor foDEntitlementDescriptor = getEntitlementDescriptor(unirest, String.valueOf(entitlementId), true);
        // check entitlement has not expired
        if (foDEntitlementDescriptor.endDate.after(Date.from(Instant.now()))) {
            progressWriter.writeI18nWarning("fcli.fod.scan-config.setup-sast.entitlement-expired");
        }
        // warn if no units are consumed or not enough for "new" scan
        if (foDEntitlementDescriptor.unitsPurchased - foDEntitlementDescriptor.getUnitsConsumed() == 0) {
            progressWriter.writeI18nWarning("fcli.fod.scan-config.setup-sast.entitlement-consumed");
        }
        // TODO: use unitsRequired to see if there are enough units left
    }

    public static final FoDEntitlementDescriptor getEntitlementDescriptor(UnirestInstance unirest, String entitlementId,
                                                                          boolean failIfNotFound) {
        Optional<JsonNode> entitlement = JsonHelper.stream(
                (ArrayNode)unirest.get(FoDUrls.ENTITLEMENTS)
                .asObject(JsonNode.class).getBody().get("tenantEntitlements")
        )
        .filter(e -> e.get("entitlementId").asText().equals(entitlementId))
        .findFirst();
        return (entitlement.isEmpty() ? getEmptyDescriptor() : getDescriptor(entitlement.get()));
    }

    //

    private final static Integer unitsRequired(FoDAssessmentType assessmentType,
                                               FoDEnums.EntitlementFrequencyType entitlementType) {
        if (entitlementType == FoDEnums.EntitlementFrequencyType.SingleScan) {
            return assessmentType.getSingleUnits();
        } else if (entitlementType == FoDEnums.EntitlementFrequencyType.Subscription) {
            return assessmentType.getSubscriptionUnits();
        } else {
            throw new IllegalArgumentException("Unknown entitlement type used: " + entitlementType.name());
        }
    }

    private static final FoDEntitlementDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDEntitlementDescriptor.class);
    }

    private static final FoDEntitlementDescriptor getEmptyDescriptor() {
        return JsonHelper.treeToValue(getObjectMapper().createObjectNode(), FoDEntitlementDescriptor.class);
    }

    //

}
