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

package com.fortify.cli.fod.entity.microservice.helper;

import static com.fortify.cli.fod.entity.app.helper.FoDAppHelper.getAppDescriptor;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod.entity.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.entity.microservice.cli.mixin.FoDAppAndMicroserviceNameDescriptor;
import com.fortify.cli.fod.rest.FoDUrls;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDAppMicroserviceHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDAppMicroserviceDescriptor getRequiredAppMicroservice(UnirestInstance unirest, String appMicroserviceNameOrId, String delimiter, String... fields) {
        FoDAppMicroserviceDescriptor descriptor = getOptionalAppMicroservice(unirest, appMicroserviceNameOrId, delimiter, fields);
        if (descriptor == null) {
            throw new ValidationException("No application microservice found for application microservice name or id: " + appMicroserviceNameOrId);
        }
        return descriptor;
    }

    public static final FoDAppMicroserviceDescriptor getOptionalAppMicroservice(UnirestInstance unirest, String appMicroserviceNameOrId, String delimiter, String... fields) {
        try {
            int microserviceId = Integer.parseInt(appMicroserviceNameOrId);
            return getOptionalAppMicroserviceFromId(unirest, microserviceId, fields);
        } catch (NumberFormatException nfe) {
            return getOptionalAppMicroserviceFromAppAndMicroserviceName(unirest, FoDAppAndMicroserviceNameDescriptor.fromCombinedAppAndMicroserviceName(appMicroserviceNameOrId, delimiter), fields);
        }
    }

    public static final FoDAppMicroserviceDescriptor getOptionalAppMicroserviceFromId(UnirestInstance unirest, int relId, String... fields) {
        GetRequest request = unirest.get(FoDUrls.MICROSERVICES)
                .queryString("filters", String.format("microserviceId:%d", relId));
        return getOptionalDescriptor(request);
    }

    public static final FoDAppMicroserviceDescriptor getOptionalAppMicroserviceFromAppAndMicroserviceName(UnirestInstance unirest, FoDAppAndMicroserviceNameDescriptor appAndMicroserviceNameDescriptor, String... fields) {
        return getAppMicroserviceDescriptor(unirest, appAndMicroserviceNameDescriptor.getAppName(), appAndMicroserviceNameDescriptor.getMicroserviceName(), true);
    }

    // TODO Consider splitting into multiple methods
    public static final FoDAppMicroserviceDescriptor getAppMicroserviceDescriptor(UnirestInstance unirest, String appName, String microserviceName, boolean failIfNotFound) {
        GetRequest request = unirest.get(FoDUrls.MICROSERVICES);
        int appId = 0;
        boolean isMicroserviceId = false;
        int microserviceId = 0;
        try {
            appId = Integer.parseInt(appName);
        } catch (NumberFormatException nfe) {
            appId = getAppDescriptor(unirest, appName, true).getApplicationId();
        }
        //System.out.println(appId);

        try {
            microserviceId = Integer.parseInt(microserviceName);
            isMicroserviceId = true;
        } catch (NumberFormatException nfe) {
            isMicroserviceId = false;
        }
        //System.out.println(microserviceId);
        request = request.routeParam("appId", String.valueOf(appId));
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && (items.size() == 0 || !items.isArray())) {
            throw new ValidationException("No application microservices found for names or ids: " + appName + ":" + microserviceName);
        }
        FoDAppMicroserviceDescriptor descriptor = new FoDAppMicroserviceDescriptor();
        for (final JsonNode objNode : items) {
            //System.out.println(objNode);
            if ((isMicroserviceId && objNode.get("microserviceId").asInt() == microserviceId) ||
                    objNode.get("microserviceName").asText().equals(microserviceName)) {
                descriptor.setMicroserviceId(objNode.get("microserviceId").asInt());
                descriptor.setMicroserviceName(objNode.get("microserviceName").asText());
                descriptor.setApplicationId(appId);
                descriptor.setReleaseId(objNode.get("releaseId").asInt());
            }
        }
        if (descriptor.getMicroserviceName() == null || descriptor.getMicroserviceName().isEmpty()) {
            if (failIfNotFound)
                throw new ValidationException("No microservice found for application and microservice name or ids: " + appName + ":" + microserviceName);
        }
        return descriptor;
    }

    public static final JsonNode createAppMicroservice(UnirestInstance unirest, Integer appId, FoDAppMicroserviceUpdateRequest msRequest) {
        ObjectNode body = objectMapper.valueToTree(msRequest);
        JsonNode response = unirest.post(FoDUrls.MICROSERVICES)
                .routeParam("appId", String.valueOf(String.valueOf(appId)))
                .body(body).asObject(JsonNode.class).getBody();
        FoDAppMicroserviceDescriptor descriptor = getDescriptor(response);
        ObjectNode node = getObjectMapper().createObjectNode();
        node.put("applicationId", appId);
        node.put("applicationName", getAppDescriptor(unirest, String.valueOf(appId), true).getApplicationName());
        node.put("microserviceId", descriptor.getMicroserviceId());
        node.put("microserviceName", msRequest.getMicroserviceName());
        return node;
    }

    public static final JsonNode updateAppMicroservice(UnirestInstance unirest, FoDAppMicroserviceDescriptor currentMs, FoDAppMicroserviceUpdateRequest msRequest) {
        ObjectNode body = objectMapper.valueToTree(msRequest);
        JsonNode response = unirest.put(FoDUrls.MICROSERVICES_UPDATE)
                .routeParam("appId", String.valueOf(currentMs.getApplicationId()))
                .routeParam("microserviceId", String.valueOf(currentMs.getMicroserviceId()))
                .body(body).asObject(JsonNode.class).getBody();
        FoDAppMicroserviceDescriptor descriptor = getDescriptor(response);
        ObjectNode node = getObjectMapper().createObjectNode();
        node.put("applicationId", currentMs.getApplicationId());
        node.put("applicationName", getAppDescriptor(unirest, String.valueOf(currentMs.getApplicationId()), true).getApplicationName());
        node.put("microserviceId", descriptor.getMicroserviceId());
        node.put("microserviceName", msRequest.getMicroserviceName());
        return node;
    }

    public static final JsonNode deleteAppMicroservice(UnirestInstance unirest, FoDAppMicroserviceDescriptor currentMs) {
        FoDAppDescriptor appDescriptor = getAppDescriptor(unirest, String.valueOf(currentMs.getApplicationId()), true);
        unirest.delete(FoDUrls.MICROSERVICES_UPDATE)
                .routeParam("appId", String.valueOf(currentMs.getApplicationId()))
                .routeParam("microserviceId", String.valueOf(currentMs.getMicroserviceId()))
                .asObject(JsonNode.class).getBody();
        ObjectNode node = getObjectMapper().createObjectNode();
        node.put("applicationName", appDescriptor.getApplicationName());
        node.put("microserviceId", currentMs.getMicroserviceId());
        node.put("microserviceName", currentMs.getMicroserviceName());
        return node;
    }

    //

    private static final FoDAppMicroserviceDescriptor getOptionalDescriptor(GetRequest request) {
        JsonNode microservices = request.asObject(ObjectNode.class).getBody().get("items");
        if (microservices.size() > 1) {
            throw new ValidationException("Multiple application microservices found");
        }
        return microservices.size() == 0 ? null : getDescriptor(microservices.get(0));
    }

    private static final FoDAppMicroserviceDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDAppMicroserviceDescriptor.class);
    }


}
