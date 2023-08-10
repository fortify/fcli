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

import static java.util.function.Predicate.not;

import java.util.Optional;

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

import kong.unirest.core.UnirestInstance;
import lombok.Getter;

// TODO Class contains unused unitsRequired() method
// TODO Class contains some fairly long methods; consider splitting methods
public class FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDScanAssessmentTypeDescriptor validateRemediationEntitlement(UnirestInstance unirest, IProgressWriterI18n progressWriter, String relId,
                                                                                   Integer entitlementId, FoDScanType scanType) {
        FoDScanAssessmentTypeDescriptor entitlement = new FoDScanAssessmentTypeDescriptor();
        FoDAssessmentTypeDescriptor[] assessmentTypeDescriptors = FoDAssessmentTypeHelper.getAssessmentTypes(unirest,
                relId, scanType, true);
        if (assessmentTypeDescriptors.length > 0) {
            progressWriter.writeI18nProgress("validating-remediation-entitlement");
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
                progressWriter.writeI18nProgress("using-remediation-entitlement", entitlement.getEntitlementDescription());
            } else {
                throw new IllegalStateException("No remediation scan entitlements found");
            }
        }
        return entitlement;
    }

    public static final FoDScanAssessmentTypeDescriptor getEntitlementToUse(UnirestInstance unirest, IProgressWriterI18n progressWriter, String relId,
                                                                        FoDAssessmentType assessmentType,
                                                                        FoDEnums.EntitlementPreferenceType entitlementType,
                                                                        FoDScanType scanType) {
        FoDScanAssessmentTypeDescriptor entitlement = new FoDScanAssessmentTypeDescriptor();
        FoDAssessmentTypeDescriptor[] assessmentTypeDescriptors = FoDAssessmentTypeHelper.getAssessmentTypes(unirest,
                relId, scanType, true);
        if (assessmentTypeDescriptors.length > 0) {
            progressWriter.writeI18nProgress("validating-entitlement");
            // check for an entitlement
            for (FoDAssessmentTypeDescriptor atd : assessmentTypeDescriptors) {
                if (atd.getEntitlementId() != null && atd.getEntitlementId() > 0) {
                    if (atd.getFrequencyType().equals(entitlementType.name().replace("Only",""))) {
                        String atdName = atd.getName()
                                .replace(" ", "")
                                .replace("+", "Plus")
                                .replace("Assessment", "");
                        if (atdName.equals(assessmentType.name())) {
                            entitlement.setEntitlementDescription(atd.getEntitlementDescription());
                            entitlement.setEntitlementId(atd.getEntitlementId());
                            entitlement.setFrequencyType(atd.getFrequencyType());
                            entitlement.setAssessmentTypeId(atd.getAssessmentTypeId());
                            entitlement.setEntitlementDescription(atd.getEntitlementDescription());
                            break;
                        }
                    }
                }
            }
            if (entitlement.getEntitlementId() != null && entitlement.getEntitlementId() > 0) {
                progressWriter.writeI18nProgress("using-entitlement", entitlement.getEntitlementDescription());
            }
        }
        return entitlement;
    }

    // TODO Unused method
    private final static Integer unitsRequired(FoDAssessmentType assessmentType,
                                               FoDEnums.EntitlementPreferenceType entitlementType) {
        if (entitlementType == FoDEnums.EntitlementPreferenceType.SingleScanOnly ||
                entitlementType == FoDEnums.EntitlementPreferenceType.SingleScanFirstThenSubscription) {
            return assessmentType.getSingleUnits();
        } else if (entitlementType == FoDEnums.EntitlementPreferenceType.SubscriptionOnly ||
                entitlementType == FoDEnums.EntitlementPreferenceType.SubscriptionFirstThenSingleScan) {
            return assessmentType.getSubscriptionUnits();
        } else {
            throw new IllegalArgumentException("Unknown entitlement type used: " + entitlementType.name());
        }
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

    //

    private static final FoDScanDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDScanDescriptor.class);
    }

    private static final FoDScanDescriptor getEmptyDescriptor() {
        return JsonHelper.treeToValue(getObjectMapper().createObjectNode(), FoDScanDescriptor.class);
    }


}
