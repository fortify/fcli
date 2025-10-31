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
package com.fortify.cli.fod.release.helper;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod._common.util.FoDEnums.EntitlementFrequencyType;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public final class FoDReleaseAssessmentTypeHelper {
    private static final Log LOG = LogFactory.getLog(FoDReleaseAssessmentTypeHelper.class);
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private FoDReleaseAssessmentTypeHelper() {}

    public static final FoDReleaseAssessmentTypeDescriptor[] getAssessmentTypes(UnirestInstance unirestInstance,
                                                                        String relId,
                                                                        FoDScanType scanType,
                                                                        FoDEnums.EntitlementFrequencyType entitlementFrequencyType,
                                                                        Boolean isRemediation,
                                                                        boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.RELEASE + "/assessment-types")
                .routeParam("relId", relId)
                .queryString("scanType", scanType.name())
                .queryString("filters", "frequencyType:"
                        .concat(entitlementFrequencyType.name())
                        .concat("+isRemediation:").concat(isRemediation.toString()));
        JsonNode assessmentTypes = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && assessmentTypes.size() == 0) {
            throw new FcliSimpleException("No assessment types found for release id: " + relId);
        }
        return JsonHelper.treeToValue(assessmentTypes, FoDReleaseAssessmentTypeDescriptor[].class);
    }

    public static final FoDReleaseAssessmentTypeDescriptor getAssessmentTypeDescriptor(
        UnirestInstance unirest, String relId, 
        FoDScanType scanType, EntitlementFrequencyType entFreqType, String assessmentType) {
        // support both assessment type id and name
        java.util.function.Predicate<FoDReleaseAssessmentTypeDescriptor> predicate;
        try {
            int assessmentTypeId = Integer.parseInt(assessmentType);
            predicate = n -> n.getAssessmentTypeId().equals(assessmentTypeId);
        } catch (NumberFormatException nfe) {
            predicate = n -> n.getName().equals(assessmentType);
        }
        return Arrays.stream(FoDReleaseAssessmentTypeHelper.getAssessmentTypes(
                    unirest, relId, scanType, entFreqType, false, true))
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Cannot find appropriate assessment type for specified options."));
    }

    // This method has caused issues if enforced (#682) so we are just using it for logging for now
    public final static boolean validateEntitlementCanBeUsed(String relId, FoDReleaseAssessmentTypeDescriptor atd) {
        if (atd == null || atd.getAssessmentTypeId() == null || atd.getAssessmentTypeId() <= 0) {
            throw new FcliSimpleException("Invalid or empty FODAssessmentTypeDescriptor.");
        }
        LOG.debug("Validating entitlement for release: " + relId);
        Date now = Date.from(Instant.now());
        Date endDate = atd.getSubscriptionEndDate();
        if (endDate != null && endDate.before(now)) {
            LOG.debug("Subscription End Date: " + endDate);
            LOG.debug("Warning: the entitlement has expired.");
            return false;
        }
        if (atd.getUnitsAvailable() == 0) {
            LOG.debug("Warning: all units of the entitlement have been consumed.");
            return false;
        }
        if (atd.getUnits() > atd.getUnitsAvailable()) {
            LOG.debug("Warning: the assessment needs more units than are available.");
            return false;
        }
        return true;
    }
}
