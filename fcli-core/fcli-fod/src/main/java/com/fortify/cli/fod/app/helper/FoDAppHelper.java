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
package com.fortify.cli.fod.app.helper;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.rest.helper.FoDDataHelper;
import com.fortify.cli.fod.app.cli.mixin.FoDAppTypeOptions.FoDAppType;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
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
        JsonNode result = null;
        try {
            int appId = Integer.parseInt(appNameOrId);
            result = FoDDataHelper.findUnique(request, String.format("applicationId:%d", appId));
        } catch (NumberFormatException nfe) {
            result = FoDDataHelper.findUnique(request, String.format("applicationName:%s", appNameOrId));
        }
        if ( failIfNotFound && result==null ) {
            throw new FcliSimpleException("No application found for name or id: " + appNameOrId);
        }
        return getAppDescriptor(result);
    }

    public static final FoDAppDescriptor createApp(UnirestInstance unirest, FoDAppCreateRequest appCreateRequest) {
        var appId = unirest.post(FoDUrls.APPLICATIONS)
                .body(appCreateRequest.asObjectNode()).asObject(JsonNode.class).getBody().get("applicationId").asText();
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

    private static final FoDAppDescriptor getAppDescriptor(JsonNode node) {
        return node==null ? null : JsonHelper.treeToValue(node, FoDAppDescriptor.class);
    }

}
