/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 ******************************************************************************/

package com.fortify.cli.fod.entity.scan.helper;

import static java.util.function.Predicate.not;

import java.util.Optional;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.progress.helper.IProgressHelperI18n;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.fod.entity.release.helper.FoDAppRelAssessmentTypeDescriptor;
import com.fortify.cli.fod.entity.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.entity.scan.cli.mixin.FoDAssessmentTypeOptions;
import com.fortify.cli.fod.entity.scan.cli.mixin.FoDScanFormatOptions;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.util.FoDEnums;

import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;

// TODO Class contains unused unitsRequired() method
// TODO Class contains some fairly long methods; consider splitting methods
public class FoDScanHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDAssessmentTypeDescriptor validateRemediationEntitlement(UnirestInstance unirest, IProgressHelperI18n progressHelper, String relId,
                                                                                   Integer entitlementId, FoDScanFormatOptions.FoDScanType scanType) {
        FoDAssessmentTypeDescriptor entitlement = new FoDAssessmentTypeDescriptor();
        FoDAppRelAssessmentTypeDescriptor[] assessmentTypeDescriptors = FoDAppRelHelper.getAppRelAssessmentTypes(unirest,
                relId, scanType, true);
        if (assessmentTypeDescriptors.length > 0) {
            progressHelper.writeI18nProgress("validating-remediation-entitlement");
            // check we have an appropriate remediation scan available
            for (FoDAppRelAssessmentTypeDescriptor atd : assessmentTypeDescriptors) {
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
                progressHelper.writeI18nProgress("using-remediation-entitlement", entitlement.getEntitlementDescription());
            } else {
                throw new ValidationException("No remediation scan entitlements found");
            }
        }
        return entitlement;
    }

    public static final FoDAssessmentTypeDescriptor getEntitlementToUse(UnirestInstance unirest, IProgressHelperI18n progressHelper, String relId,
                                                                        FoDAssessmentTypeOptions.FoDAssessmentType assessmentType,
                                                                        FoDEnums.EntitlementPreferenceType entitlementType,
                                                                        FoDScanFormatOptions.FoDScanType scanType) {
        FoDAssessmentTypeDescriptor entitlement = new FoDAssessmentTypeDescriptor();
        FoDAppRelAssessmentTypeDescriptor[] assessmentTypeDescriptors = FoDAppRelHelper.getAppRelAssessmentTypes(unirest,
                relId, scanType, true);
        if (assessmentTypeDescriptors.length > 0) {
            progressHelper.writeI18nProgress("validating-entitlement");
            // check for an entitlement
            for (FoDAppRelAssessmentTypeDescriptor atd : assessmentTypeDescriptors) {
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
                progressHelper.writeI18nProgress("using-entitlement", entitlement.getEntitlementDescription());
            }
        }
        return entitlement;
    }

    // TODO Unused method
    private final static Integer unitsRequired(FoDAssessmentTypeOptions.FoDAssessmentType assessmentType,
                                               FoDEnums.EntitlementPreferenceType entitlementType) {
        if (entitlementType == FoDEnums.EntitlementPreferenceType.SingleScanOnly ||
                entitlementType == FoDEnums.EntitlementPreferenceType.SingleScanFirstThenSubscription) {
            return assessmentType.getSingleUnits();
        } else if (entitlementType == FoDEnums.EntitlementPreferenceType.SubscriptionOnly ||
                entitlementType == FoDEnums.EntitlementPreferenceType.SubscriptionFirstThenSingleScan) {
            return assessmentType.getSubscriptionUnits();
        } else {
            throw new ValidationException("Unknown entitlement type used: " + entitlementType.name());
        }
    }

    public static final FoDScanDescriptor getScanDescriptor(UnirestInstance unirest, String scanId) throws FoDScanNotFoundException {
        try {
            HttpResponse<ObjectNode> response = unirest.get(FoDUrls.SCAN + "/summary")
                    .routeParam("scanId", scanId).asObject(ObjectNode.class);
            if (response.isSuccess()) {
                JsonNode scan = response.getBody();
                return scan == null ? null : getDescriptor(scan);
            }
        } catch (UnexpectedHttpResponseException ex) {
            if (ex.getMessage().contains("404 Not Found")) {
                throw new FoDScanNotFoundException("Could not retrieve scan with id: " + scanId);
            }
        }
        return null;
    }

    public static final FoDScanDescriptor getLatestScanDescriptor(UnirestInstance unirest, String relId,
                                                                  FoDScanFormatOptions.FoDScanType scanType,
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
