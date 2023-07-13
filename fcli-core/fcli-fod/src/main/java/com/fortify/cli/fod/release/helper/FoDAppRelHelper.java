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

import java.util.List;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.release.cli.mixin.FoDAppAndRelNameDescriptor;
import com.fortify.cli.fod.release.cli.mixin.FoDAppMicroserviceAndRelNameDescriptor;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDAppRelHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDAppRelDescriptor getRequiredAppRel(UnirestInstance unirest, String appRelNameOrId, String delimiter, String... fields) {
        FoDAppRelDescriptor descriptor = getOptionalAppRel(unirest, appRelNameOrId, delimiter, fields);
        if (descriptor == null) {
            throw new ValidationException("No application release found for application release name or id: " + appRelNameOrId);
        }
        return descriptor;
    }

    public static final FoDAppRelDescriptor getRequiredAppMicroserviceRel(UnirestInstance unirest, String appMicroserviceRelNameOrId, String delimiter, String... fields) {
        FoDAppRelDescriptor descriptor = getOptionalAppMicroserviceRel(unirest, appMicroserviceRelNameOrId, delimiter, fields);
        if (descriptor == null) {
            throw new ValidationException("No application microservice release found for application microservice release name or id: " + appMicroserviceRelNameOrId);
        }
        return descriptor;
    }

    public static final FoDAppRelDescriptor getOptionalAppRel(UnirestInstance unirest, String appRelNameOrId, String delimiter, String... fields) {
        try {
            int relId = Integer.parseInt(appRelNameOrId);
            return getOptionalAppRelFromId(unirest, relId, fields);
        } catch (NumberFormatException nfe) {
            return getOptionalAppRelFromAppAndRelName(unirest, FoDAppAndRelNameDescriptor.fromCombinedAppAndRelName(appRelNameOrId, delimiter), fields);
        }
    }

    public static final FoDAppRelDescriptor getOptionalAppMicroserviceRel(UnirestInstance unirest, String appMicroserviceRelNameOrId, String delimiter, String... fields) {
        try {
            int relId = Integer.parseInt(appMicroserviceRelNameOrId);
            return getOptionalAppRelFromId(unirest, relId, fields);
        } catch (NumberFormatException nfe) {
            return getOptionalAppMicroserviceRelFromAppAndRelName(unirest, FoDAppMicroserviceAndRelNameDescriptor.fromCombinedAppMicroserviceAndRelName(appMicroserviceRelNameOrId, delimiter), fields);
        }
    }

    public static final FoDAppRelDescriptor getOptionalAppRelFromId(UnirestInstance unirest, int relId, String... fields) {
        GetRequest request = unirest.get(FoDUrls.RELEASES)
                .queryString("filters", String.format("releaseId:%d", relId));
        return getOptionalDescriptor(request);
    }

    public static final FoDAppRelDescriptor getOptionalAppRelFromAppAndRelName(UnirestInstance unirest, FoDAppAndRelNameDescriptor appAndRelNameDescriptor, String... fields) {
        GetRequest request = unirest.get(FoDUrls.RELEASES)
                .queryString("filters", String.format("applicationName:%s+releaseName:%s", appAndRelNameDescriptor.getAppName(), appAndRelNameDescriptor.getRelName()));
        return getOptionalDescriptor(request);
    }

    public static final FoDAppRelDescriptor getOptionalAppMicroserviceRelFromAppAndRelName(UnirestInstance unirest, FoDAppMicroserviceAndRelNameDescriptor appMicroserviceAndRelNameDescriptor, String... fields) {
        GetRequest request = unirest.get(FoDUrls.RELEASES)
                .queryString("filters", String.format("applicationName:%s+microserviceName:%s+releaseName:%s", appMicroserviceAndRelNameDescriptor.getAppName(), appMicroserviceAndRelNameDescriptor.getMicroserviceName(), appMicroserviceAndRelNameDescriptor.getRelName()));
        return getOptionalDescriptor(request);
    }

    // TODO Consider splitting into multiple methods
    public static final FoDAppRelDescriptor getAppRelDescriptor(UnirestInstance unirest, String appAndMsAndRelNameOrId, String delimiter, boolean failIfNotFound) {
        String[] appAndMsAndRelName = appAndMsAndRelNameOrId.split(delimiter);
        GetRequest request = unirest.get(FoDUrls.RELEASES);
        int appId = 0, relId = 0, msId = 0;
        if (appAndMsAndRelName.length == 1) {
            // only release Id specified
            try {
                relId = Integer.parseInt(appAndMsAndRelName[0]);
                request = request.queryString("filters", String.format("releaseId:%d", relId));
            } catch (NumberFormatException nfe) {
                throw new ValidationException("If application name is not specified then the release must be an id: " + relId);
            }
        } else if (appAndMsAndRelName.length == 2) {
            // application and release name/id specified
            try {
                appId = Integer.parseInt(appAndMsAndRelName[0]);
            } catch (NumberFormatException nfe) { /* ignore */ }
            try {
                relId = Integer.parseInt(appAndMsAndRelName[1]);
            } catch (NumberFormatException nfe) { /* ignore */ }
            request = request.queryString("filters", String.format("%s:%s+%s:%s",
                    (appId > 0 ? "applicationId" : "applicationName"),
                    (appId > 0 ? String.valueOf(appId) : appAndMsAndRelName[0]),
                    (relId > 0 ? "releaseId" : "releaseName"),
                    (relId > 0 ? String.valueOf(relId) : appAndMsAndRelName[1])));
        } else  {
            // application and release name/id specified
            try {
                appId = Integer.parseInt(appAndMsAndRelName[0]);
            } catch (NumberFormatException nfe) { /* ignore */ }
            try {
                relId = Integer.parseInt(appAndMsAndRelName[1]);
            } catch (NumberFormatException nfe) { /* ignore */ }
            try {
                msId = Integer.parseInt(appAndMsAndRelName[2]);
            } catch (NumberFormatException nfe) { /* ignore */ }
            request = request.queryString("filters", String.format("%s:%s+%s:%s+%s:%s",
                (appId > 0 ? "applicationId" : "applicationName"),
                (appId > 0 ? String.valueOf(appId) : appAndMsAndRelName[0]),
                (relId > 0 ? "releaseId" : "releaseName"),
                (relId > 0 ? String.valueOf(relId) : appAndMsAndRelName[1]),
                (msId > 0 ? "microserviceId" : "microserviceName"),
                (msId > 0 ? String.valueOf(msId) : appAndMsAndRelName[2])));
        }
        JsonNode rel = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && rel.size() == 0) {
            throw new ValidationException("No application release found for names or ids: " + appAndMsAndRelName
                    + ":" + appAndMsAndRelName[2]);
        } else if (rel.size() > 1) {
            throw new ValidationException("Multiple application releases found for names or ids: " + appAndMsAndRelName
                    + ":" + appAndMsAndRelName[2]);
        }
        return rel.size() == 0 ? null : getDescriptor(rel.get(0));
    }

    public static final FoDAppRelDescriptor getAppRelDescriptorById(UnirestInstance unirest, String relId, boolean failIfNotFound) {
        GetRequest request = unirest.get(FoDUrls.RELEASE).routeParam("relId", relId);
        JsonNode rel = request.asObject(ObjectNode.class).getBody();
        if (failIfNotFound && rel.get("releaseName").asText().isEmpty()) {
            throw new ValidationException("No application release found for id: " + relId);
        }
        return getDescriptor(rel);
    }

    public static final FoDAppRelDescriptor getOptionalAppRelFromMicroserviceAndRelName(UnirestInstance unirest, String appAndRelNameOrId, String microserviceName, String delimiter, String... fields) {
        return getAppRelDescriptor(unirest, appAndRelNameOrId.concat(delimiter).concat(microserviceName), delimiter, false);
    }

    public static final FoDAppRelAssessmentTypeDescriptor[] getAppRelAssessmentTypes(UnirestInstance unirestInstance,
                                                                                     String relId, FoDScanTypeOptions.FoDScanType scanType, boolean failIfNotFound) {
        GetRequest request = unirestInstance.get(FoDUrls.RELEASE + "/assessment-types")
                .routeParam("relId", relId)
                .queryString("scanType", scanType.name());
        JsonNode assessmentTypes = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && assessmentTypes.size() == 0) {
            throw new ValidationException("No assessment types found for release id: " + relId);
        }
        return JsonHelper.treeToValue(assessmentTypes, FoDAppRelAssessmentTypeDescriptor[].class);
    }

    public static final FoDAppRelAssessmentTypeDescriptor getAppRelAssessmentType(UnirestInstance unirestInstance,
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
        return JsonHelper.treeToValue(assessmentTypes, FoDAppRelAssessmentTypeDescriptor.class);
    }

    public static final FoDAppRelDescriptor createAppRel(UnirestInstance unirest, FoDAppRelCreateRequest relCreateRequest) {
        ObjectNode body = objectMapper.valueToTree(relCreateRequest);
        JsonNode response = unirest.post(FoDUrls.RELEASES)
                .body(body).asObject(JsonNode.class).getBody();
        FoDAppRelDescriptor descriptor = JsonHelper.treeToValue(response, FoDAppRelDescriptor.class);
        return getAppRelDescriptorById(unirest, String.valueOf(descriptor.getReleaseId()), true);
    }

    public static final FoDAppRelDescriptor updateAppRel(UnirestInstance unirest, Integer relId,
                                                   FoDAppRelUpdateRequest appUpdateRequest) {
        ObjectNode body = objectMapper.valueToTree(appUpdateRequest);
        unirest.put(FoDUrls.RELEASE)
                .routeParam("relId", String.valueOf(relId))
                .body(body).asObject(JsonNode.class).getBody();
        return getAppRelDescriptorById(unirest, String.valueOf(relId), true);
    }

    public static boolean missing(List<?> list) {
        return list == null || list.isEmpty();
    }

    //

    private static final FoDAppRelDescriptor getOptionalDescriptor(GetRequest request) {
        JsonNode releases = request.asObject(ObjectNode.class).getBody().get("items");
        if (releases.size() > 1) {
            throw new ValidationException("Multiple application releases found");
        }
        return releases.size() == 0 ? null : getDescriptor(releases.get(0));
    }

    private static final FoDAppRelDescriptor getDescriptor(JsonNode node) {
        return  JsonHelper.treeToValue(node, FoDAppRelDescriptor.class);
    }


}
