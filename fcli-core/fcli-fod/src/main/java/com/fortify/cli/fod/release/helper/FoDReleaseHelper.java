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

package com.fortify.cli.fod.release.helper;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDReleaseHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDReleaseDescriptor getRequiredReleaseDescriptor(UnirestInstance unirest, String qualifiedReleaseNameOrId, String delimiter, String... fields) {
        FoDReleaseDescriptor descriptor = getOptionalReleaseDescriptor(unirest, qualifiedReleaseNameOrId, delimiter, fields);
        if (descriptor == null) {
            throw new ValidationException("No release found for name or id: " + qualifiedReleaseNameOrId);
        }
        return descriptor;
    }

    public static final FoDReleaseDescriptor getOptionalReleaseDescriptor(UnirestInstance unirest, String qualifiedReleaseNameOrId, String delimiter, String... fields) {
        try {
            int relId = Integer.parseInt(qualifiedReleaseNameOrId);
            return getOptionalReleaseDescriptorFromId(unirest, relId, fields);
        } catch (NumberFormatException nfe) {
            return getOptionalReleaseDescriptorFromQualifiedName(unirest, FoDQualifiedReleaseNameDescriptor.fromQualifiedReleaseName(qualifiedReleaseNameOrId, delimiter), fields);
        }
    }

    public static final FoDReleaseDescriptor getOptionalReleaseDescriptorFromId(UnirestInstance unirest, int relId, String... fields) {
        GetRequest request = unirest.get(FoDUrls.RELEASES)
                .queryString("filters", String.format("releaseId:%d", relId));
        return getOptionalDescriptor(request);
    }

    public static final FoDReleaseDescriptor getOptionalReleaseDescriptorFromQualifiedName(UnirestInstance unirest, FoDQualifiedReleaseNameDescriptor releaseNameDescriptor, String... fields) {
        var filters = String.format("applicationName:%s+releaseName:%s", releaseNameDescriptor.getAppName(), releaseNameDescriptor.getReleaseName());
        if ( StringUtils.isNotBlank(releaseNameDescriptor.getMicroserviceName()) ) {
            filters += String.format("+microserviceName:%s", releaseNameDescriptor.getMicroserviceName());
        }
        GetRequest request = unirest.get(FoDUrls.RELEASES).queryString("filters", filters);
        return getOptionalDescriptor(request);
    }

    public static final FoDReleaseAssessmentTypeDescriptor[] getAppRelAssessmentTypes(UnirestInstance unirestInstance,
                                                                                     String relId, FoDScanTypeOptions.FoDScanType scanType, boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.RELEASE + "/assessment-types")
                .routeParam("relId", relId)
                .queryString("scanType", scanType.name());
        JsonNode assessmentTypes = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && assessmentTypes.size() == 0) {
            throw new ValidationException("No assessment types found for release id: " + relId);
        }
        return JsonHelper.treeToValue(assessmentTypes, FoDReleaseAssessmentTypeDescriptor[].class);
    }

    public static final FoDReleaseAssessmentTypeDescriptor getAppRelAssessmentType(UnirestInstance unirestInstance,
                                                                                  String relId, FoDScanTypeOptions.FoDScanType scanType,
                                                                                  boolean isPlus, boolean failIfNotFound) {
        String filterString = "name:" + scanType.toString() +
                (isPlus ? "+" : "") + " Assessment";
        GetRequest request = unirestInstance.get(FoDUrls.RELEASE + "/assessment-types")
                .routeParam("relId", relId)
                .queryString("scanType", scanType.name())
                .queryString("filters", filterString);
        JsonNode assessmentTypes = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && assessmentTypes.size() == 0) {
            throw new ValidationException("No assessment types found for release id: " + relId);
        }
        return JsonHelper.treeToValue(assessmentTypes, FoDReleaseAssessmentTypeDescriptor.class);
    }

    public static final FoDReleaseDescriptor createRelease(UnirestInstance unirest, FoDReleaseCreateRequest relCreateRequest) {
        ObjectNode body = objectMapper.valueToTree(relCreateRequest);
        JsonNode response = unirest.post(FoDUrls.RELEASES)
                .body(body).asObject(JsonNode.class).getBody();
        return JsonHelper.treeToValue(response, FoDReleaseDescriptor.class);
    }

    public static final FoDReleaseDescriptor updateRelease(UnirestInstance unirest, String relId,
                                                   FoDReleaseUpdateRequest appUpdateRequest) {
        ObjectNode body = objectMapper.valueToTree(appUpdateRequest);
        // TODO Check whether put request doesn't already return release data
        unirest.put(FoDUrls.RELEASE)
                .routeParam("relId", relId)
                .body(body).asObject(JsonNode.class).getBody();
        return getRequiredReleaseDescriptor(unirest, relId, ":");
    }

    private static final FoDReleaseDescriptor getOptionalDescriptor(GetRequest request) {
        JsonNode releases = request.asObject(ObjectNode.class).getBody().get("items");
        if (releases.size() > 1) {
            throw new ValidationException("Multiple application releases found");
        }
        return releases.size() == 0 ? null : getDescriptor(releases.get(0));
    }

    private static final FoDReleaseDescriptor getDescriptor(JsonNode node) {
        return  JsonHelper.treeToValue(node, FoDReleaseDescriptor.class);
    }
}
