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

package com.fortify.cli.fod.sast_scan.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod.release.helper.FoDAppRelAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.rest.helper.FoDFileTransferHelper;
import com.fortify.cli.fod.scan.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.scan.cli.mixin.FoDAssessmentTypeOptions.FoDAssessmentType;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDStartScanResponse;
import com.fortify.cli.fod.util.FoDEnums;
import kong.unirest.GetRequest;
import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import javax.validation.ValidationException;
import java.io.File;

public class FoDSastScanHelper extends FoDScanHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    /*public static final FoDDastScanSetupDescriptor setupScan(UnirestInstance unirest, Integer relId, FoDSetupDastScanRequest setupDastScanRequest) {
        ObjectNode body = objectMapper.valueToTree(setupDastScanRequest);
        FoDQueryHelper.stripNulls(body);
        System.out.println(body.toPrettyString());
        JsonNode response = unirest.put(FoDUrls.DYNAMIC_SCANS + "/scan-setup")
                .routeParam("relId", String.valueOf(relId))
                .body(body).asObject(JsonNode.class).getBody();
        return JsonHelper.treeToValue(response, FoDDastScanSetupDescriptor.class);
    }*/

    public static final FoDScanDescriptor startScan(UnirestInstance unirest, String relId, FoDStartSastScanRequest req, File scanFile) {
        HttpRequest request = unirest.post(FoDUrls.STATIC_SCAN_START).routeParam("relId", relId)
                .queryString("entitlementPreferenceType", (req.getEntitlementPreferenceType() != null ?
                        FoDEnums.EntitlementPreferenceType.valueOf(req.getEntitlementPreferenceType()) : FoDEnums.EntitlementPreferenceType.SubscriptionFirstThenSingleScan))
                .queryString("purchaseEntitlement", Boolean.toString(req.getPurchaseEntitlement()))
                .queryString("remdiationScanPreferenceType", (req.getRemdiationScanPreferenceType() != null ?
                        FoDEnums.RemediationScanPreferenceType.valueOf(req.getRemdiationScanPreferenceType()) : FoDEnums.RemediationScanPreferenceType.NonRemediationScanOnly))
                .queryString("inProgressScanActionType", (req.getInProgressScanActionType() != null ?
                        FoDEnums.InProgressScanActionType.valueOf(req.getInProgressScanActionType()) : FoDEnums.InProgressScanActionType.DoNotStartScan))
                .queryString("scanTool", "fcli")
                .queryString("scanToolVersion", "Unknown")
                .queryString("scanMethodType", "Other");
        if (req.getEntitlementId() != null && req.getEntitlementId() > 0) {
            request = request.queryString("entitlementId", req.getEntitlementId());
        }
        if (req.getNotes() != null && !req.getNotes().isEmpty()) {
            // TODO: abbreviate notes
            request = request.queryString("notes", req.getNotes());
        }
        FoDStartScanResponse descriptor = FoDFileTransferHelper.startScan(unirest, request.getUrl(), scanFile.getPath());
        //System.out.println(descriptor);
        // TODO: wait until scan is being "executed" otherwise a 404?
        return getScanDescriptor(unirest, String.valueOf(descriptor.getScanId()));
    }

    public static final FoDSastScanSetupDescriptor getSetupDescriptor(UnirestInstance unirest, String relId) {
        GetRequest request = unirest.get(FoDUrls.STATIC_SCANS + "/scan-setup")
                .routeParam("relId", relId);
        JsonNode setup = request.asObject(ObjectNode.class).getBody();
        return JsonHelper.treeToValue(setup, FoDSastScanSetupDescriptor.class);
    }

    public static final FoDAssessmentTypeDescriptor validateRemediationEntitlement(UnirestInstance unirest, String relId, Integer entitlementId) {
        FoDAssessmentTypeDescriptor entitlement = new FoDAssessmentTypeDescriptor();
        FoDAppRelAssessmentTypeDescriptor[] assessmentTypeDescriptors = FoDAppRelHelper.getAppRelAssessmentTypes(unirest,
                relId, FoDScanTypeOptions.FoDScanType.Dynamic, true);
        // TODO: simplify by checking isRemediationScan?
        if (assessmentTypeDescriptors.length > 0) {
            System.out.println("Validating remediation entitlement...");
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
                System.out.println("Running remediation scan using entitlement: " + entitlement.getEntitlementDescription());
            } else {
                throw new ValidationException("No remediation scan entitlements found");
            }
        }
        return entitlement;
    }

    public static final FoDAssessmentTypeDescriptor getEntitlementToUse(UnirestInstance unirest, String relId,
                                                                       FoDAssessmentType assessmentType,
                                                                       FoDEnums.EntitlementFrequencyTypes entitlementType) {
        FoDAssessmentTypeDescriptor entitlement = new FoDAssessmentTypeDescriptor();
        FoDAppRelAssessmentTypeDescriptor[] assessmentTypeDescriptors = FoDAppRelHelper.getAppRelAssessmentTypes(unirest,
                relId, FoDScanTypeOptions.FoDScanType.Dynamic, true);
        if (assessmentTypeDescriptors.length > 0) {
            System.out.println("Validating entitlements...");
            // check for an entitlement with sufficient units available
            for (FoDAppRelAssessmentTypeDescriptor atd : assessmentTypeDescriptors) {
                if (atd.getEntitlementId() != null && atd.getEntitlementId() > 0
                        && atd.getFrequencyType().equals(entitlementType.name())
                        && atd.getUnitsAvailable() >= unitsRequired(assessmentType, entitlementType)) {
                    entitlement.setEntitlementDescription(atd.getEntitlementDescription());
                    entitlement.setEntitlementId(atd.getEntitlementId());
                    entitlement.setFrequencyType(atd.getFrequencyType());
                    entitlement.setAssessmentTypeId(atd.getAssessmentTypeId());
                    break;
                }
            }
            if (entitlement.getEntitlementId() != null && entitlement.getEntitlementId() > 0) {
                System.out.println("Running scan using entitlement: " + entitlement.getEntitlementDescription());
            }
        }
        return entitlement;
    }

    private final static Integer unitsRequired(FoDAssessmentType assessmentType, FoDEnums.EntitlementFrequencyTypes entitlementType) {
        if (entitlementType == FoDEnums.EntitlementFrequencyTypes.SingleScan) {
            return assessmentType.getSingleUnits();
        } else if (entitlementType == FoDEnums.EntitlementFrequencyTypes.Subscription) {
            return assessmentType.getSubscriptionUnits();
        } else {
            throw new ValidationException("Unknown entitlement type used: " + entitlementType.name());
        }
    }
}
