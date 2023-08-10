/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.fod.assessment_type.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.scan.helper.FoDScanType;

import kong.unirest.core.GetRequest;
import kong.unirest.core.UnirestInstance;

public final class FoDAssessmentTypeHelper {
    private FoDAssessmentTypeHelper() {}

    public static final FoDAssessmentTypeDescriptor[] getAssessmentTypes(UnirestInstance unirestInstance,
                                                                                     String relId, FoDScanType scanType, boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.RELEASE + "/assessment-types")
                .routeParam("relId", relId)
                .queryString("scanType", scanType.name());
        JsonNode assessmentTypes = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && assessmentTypes.size() == 0) {
            throw new IllegalStateException("No assessment types found for release id: " + relId);
        }
        return JsonHelper.treeToValue(assessmentTypes, FoDAssessmentTypeDescriptor[].class);
    }
}
