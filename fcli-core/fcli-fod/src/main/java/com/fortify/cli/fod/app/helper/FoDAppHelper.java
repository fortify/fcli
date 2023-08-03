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
package com.fortify.cli.fod.app.helper;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

// TODO Review method length (avoid long methods), in particular updateApp()
public class FoDAppHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDAppDescriptor getAppDescriptor(UnirestInstance unirest, String appNameOrId, boolean failIfNotFound) {
        GetRequest request = unirest.get(FoDUrls.APPLICATIONS);
        try {
            int appId = Integer.parseInt(appNameOrId);
            request = request.queryString("filters", String.format("applicationId:%d", appId));
        } catch (NumberFormatException nfe) {
            request = request.queryString("filters", String.format("applicationName:%s", appNameOrId));
        }
        JsonNode app = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && app.size() == 0) {
            throw new ValidationException("No application found for name or id: " + appNameOrId);
        } else if (app.size() > 1) {
            throw new ValidationException("Multiple applications found for name or id: " + appNameOrId);
        }
        return app.size() == 0 ? null : getDescriptor(app.get(0));
    }

    public static final FoDAppDescriptor createApp(UnirestInstance unirest, FoDAppCreateRequest appCreateRequest) {
        ObjectNode body = objectMapper.valueToTree(appCreateRequest);
        // if microservice, remove applicationType field and set releaseMicroserviceName if not already set
        if (appCreateRequest.getHasMicroservices()) {
            body.remove("applicationType");
            if (StringUtils.isBlank(appCreateRequest.getReleaseMicroserviceName())) {
                body.replace("releaseMicroserviceName", appCreateRequest.getMicroservices().get(0));
            }
        }
        JsonNode response = unirest.post(FoDUrls.APPLICATIONS)
                .body(body).asObject(JsonNode.class).getBody();
        FoDAppDescriptor descriptor = JsonHelper.treeToValue(response, FoDAppDescriptor.class);
        descriptor.asObjectNode()
                .put("applicationName", appCreateRequest.getApplicationName())
                .put("releaseName", appCreateRequest.getReleaseName())
                .put("microserviceName", appCreateRequest.getMicroservices().get(0).asText())
                .put("applicationType", appCreateRequest.getHasMicroservices() ? FoDAppTypeOptions.FoDAppType.Microservice.getName() : appCreateRequest.getApplicationType())
                .put("businessCriticalityType", appCreateRequest.getBusinessCriticalityType())
                .put("applicationDescription", appCreateRequest.getApplicationDescription());
        return descriptor;
    }

    public static final FoDAppDescriptor updateApp(UnirestInstance unirest, String appId,
                                                   FoDAppUpdateRequest appUpdateRequest) {
        ObjectNode body = objectMapper.valueToTree(appUpdateRequest);
        unirest.put(FoDUrls.APPLICATION)
                .routeParam("appId", appId)
                .body(body).asObject(JsonNode.class).getBody();

        return getAppDescriptor(unirest, appId, true);
    }

    public static String getEmailList(ArrayList<String> notifications) {
        if (notifications != null && !notifications.isEmpty()) {
            return String.join(",", notifications);
        } else {
            return "";
        }
    }

    public static JsonNode getApplicationsNode(UnirestInstance unirest, ArrayList<String> applications) {
        ArrayNode appArray = getObjectMapper().createArrayNode();
        if (applications == null || applications.isEmpty()) return appArray;
        for (String a : applications) {
            FoDAppDescriptor appDescriptor = FoDAppHelper.getAppDescriptor(unirest, a, true);
            appArray.add(appDescriptor.getApplicationId());
        }
        return appArray;
    }

    public static JsonNode getMicroservicesNode(List<String> microservices) {
        ArrayNode microserviceArray = objectMapper.createArrayNode();
        if (microservices == null || microservices.isEmpty()) return microserviceArray;
        for (String ms : microservices) {
            microserviceArray.add(ms);
        }
        return microserviceArray;
    }

    private static final FoDAppDescriptor getDescriptor(JsonNode node) {
        return  JsonHelper.treeToValue(node, FoDAppDescriptor.class);
    }

}
