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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions.FoDAppType;

import kong.unirest.core.GetRequest;
import kong.unirest.core.UnirestInstance;
import lombok.Getter;

public class FoDAppHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode transformRecord(JsonNode record) {
        return addFcliAppType((ObjectNode)record);
    }

    private static JsonNode addFcliAppType(ObjectNode record) {
        FoDAppType type = null;
        if (record.get("hasMicroservices").asBoolean()) {
            type = FoDAppType.Microservice;
        } else {
            type = FoDAppType.fromFoDValue(record.get("applicationType").asText());
        }
        return record.put("fcliApplicationType", type.getFriendlyName());
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
            throw new IllegalArgumentException("No application found for name or id: " + appNameOrId);
        } else if (app.size() > 1) {
            throw new IllegalArgumentException("Multiple applications found for name or id: " + appNameOrId);
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
        var appId = unirest.post(FoDUrls.APPLICATIONS)
                .body(body).asObject(JsonNode.class).getBody().get("applicationId").asText();
        return getAppDescriptor(unirest, appId, true);
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
