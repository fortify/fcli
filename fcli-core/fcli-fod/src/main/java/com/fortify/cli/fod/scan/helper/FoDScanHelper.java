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

package com.fortify.cli.fod.scan.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.assessment_type.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.assessment_type.helper.FoDAssessmentTypeHelper;
import com.fortify.cli.fod.entitlement.helper.FoDEntitlementHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupDescriptor;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupHelper;
import com.fortify.cli.fod.rest.lookup.helper.FoDLookupType;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

import static java.util.function.Predicate.not;

// TODO Class contains some fairly long methods; consider splitting methods
public class FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDScanAssessmentTypeDescriptor validateRemediationEntitlement(UnirestInstance unirest, IProgressWriterI18n progressWriter, String relId,
                                                                                       FoDEnums.EntitlementFrequencyType entitlementFrequencyType,
                                                                                   Integer entitlementId, FoDScanType scanType) {
        FoDScanAssessmentTypeDescriptor entitlement = new FoDScanAssessmentTypeDescriptor();
        FoDAssessmentTypeDescriptor[] assessmentTypeDescriptors = FoDAssessmentTypeHelper.getAssessmentTypes(unirest,
                relId, scanType, entitlementFrequencyType, true);
        if (assessmentTypeDescriptors.length > 0) {
            progressWriter.writeI18nProgress("fcli.fod.scan.start-sast.validating-remediation-entitlement");
            // check we have an appropriate remediation scan available
            for (FoDAssessmentTypeDescriptor atd : assessmentTypeDescriptors) {
                if (atd.getEntitlementId() > 0 && atd.getEntitlementId().equals(entitlementId) && atd.getIsRemediation()
                        && atd.getRemediationScansAvailable() > 0) {
                    entitlement.setEntitlementDescription(atd.getEntitlementDescription());
                    entitlement.setEntitlementId(atd.getEntitlementId());
                    entitlement.setFrequencyType(atd.getFrequencyType());
                    entitlement.setAssessmentTypeId(atd.getAssessmentTypeId());
                    break;
                }
            }
            if (entitlement.getEntitlementId() != null && entitlement.getEntitlementId() > 0) {
                progressWriter.writeI18nProgress("fcli.fod.scan.start-sast.using-remediation-entitlement", entitlement.getEntitlementDescription());
            } else {
                throw new IllegalStateException("No remediation scan entitlements found");
            }
        }
        return entitlement;
    }

    public static final FoDScanAssessmentTypeDescriptor getEntitlementToUse(UnirestInstance unirest, IProgressWriterI18n progressWriter, String relId,
                                                                        String assessmentType, FoDEnums.EntitlementPreferenceType entitlementType,
                                                                        FoDScanType scanType) {
        FoDScanAssessmentTypeDescriptor entitlement = new FoDScanAssessmentTypeDescriptor();
        FoDEnums.EntitlementFrequencyType frequencyType = entitlementType.toFrequencyType();
        FoDAssessmentTypeDescriptor[] assessmentTypeDescriptors = FoDAssessmentTypeHelper.getAssessmentTypes(unirest,
                relId, scanType, frequencyType, true);
        System.out.println(Arrays.toString(assessmentTypeDescriptors));
        if (assessmentTypeDescriptors.length > 0) {
            progressWriter.writeI18nProgress("fcli.fod.scan.start-sast.validating-entitlement");
            // check for an entitlement
            for (FoDAssessmentTypeDescriptor atd : assessmentTypeDescriptors) {
                if (atd.getEntitlementId() != null && atd.getEntitlementId() > 0) {
                    if (atd.getName().equals(assessmentType)) {
                        entitlement.setEntitlementDescription(atd.getEntitlementDescription());
                        entitlement.setEntitlementId(atd.getEntitlementId());
                        entitlement.setFrequencyType(atd.getFrequencyType());
                        entitlement.setAssessmentTypeId(atd.getAssessmentTypeId());
                        entitlement.setEntitlementDescription(atd.getEntitlementDescription());
                        break;
                    }
                }
            }
            if (entitlement.getEntitlementId() != null && entitlement.getEntitlementId() > 0) {
                progressWriter.writeI18nProgress("fcli.fod.scan.start-sast.using-entitlement", entitlement.getEntitlementDescription());
            }
        }
        return entitlement;
    }

    public final static Integer findEntitlementIdToUse(UnirestInstance unirest, IProgressWriterI18n progressWriter,
                                                       String relId, String assessmentType,
                                                       FoDEnums.EntitlementFrequencyType frequencyType,
                                                       FoDScanType scanType) {
        FoDAssessmentTypeDescriptor[] assessmentTypeDescriptors = FoDAssessmentTypeHelper.getAssessmentTypes(unirest,
                relId, scanType, frequencyType, true);
        Integer entitlementIdToUse = 0;
        if (assessmentTypeDescriptors.length > 0) {
            for (FoDAssessmentTypeDescriptor atd : assessmentTypeDescriptors) {
                if (atd.getEntitlementId() != null && atd.getEntitlementId() > 0) {
                    if (atd.getName().equals(assessmentType)) {
                        entitlementIdToUse = atd.getEntitlementId();
                        // validate entitlement - only warn for now
                        FoDEntitlementHelper.validateEntitlement(unirest, progressWriter, entitlementIdToUse);
                        break;
                    }
                }
            }
        }
        return entitlementIdToUse;
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

    //

    private static final FoDScanDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }

    private static final FoDScanDescriptor getEmptyDescriptor() {
        return JsonHelper.treeToValue(getObjectMapper().createObjectNode(), FoDScanDescriptor.class);
    }


}
