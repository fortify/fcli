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
package com.fortify.cli.fod.appmicroservice.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import javax.validation.ValidationException;
import java.util.Iterator;
import java.util.List;

public class FoDAppMicroserviceHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[] {
                "microserviceId:id", "microserviceName:name"
        }).transform(record);
    }

    public static final FoDAppMicroserviceDescriptor getAppMicroservice(UnirestInstance unirest, String appAndMicroserviceNameOrId, String delimiter, boolean failIfNotFound) throws JsonProcessingException {
        String[] appAndMicroserviceName = appAndMicroserviceNameOrId.split(delimiter);
        GetRequest request = unirest.get(FoDUrls.MICROSERVICES);
        int appId = 0;
        boolean isMicroserviceId = false; int microserviceId = 0;
        if (appAndMicroserviceName.length < 2) {
            throw new ValidationException("Both an application name/id and microservice name need to be specified/id: " + appAndMicroserviceNameOrId);
        }
        try {
            appId = Integer.parseInt(appAndMicroserviceName[0]);
        } catch (NumberFormatException nfe) {
            appId = FoDAppHelper.getApp(unirest, appAndMicroserviceName[0], true).getApplicationId();
        }
        try {
            microserviceId = Integer.parseInt(appAndMicroserviceName[1]); isMicroserviceId = true;
        } catch (NumberFormatException nfe) {
            isMicroserviceId = false;
        }
        request = request.routeParam("appId", String.valueOf(appId));
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        if (failIfNotFound && items.size() == 0) {
            throw new ValidationException("No application microservices found for names or ids: " + appAndMicroserviceNameOrId);
        }
        List<FoDAppMicroserviceDescriptor> appMsList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDAppMicroserviceDescriptor>>() {
                });
        Iterator<FoDAppMicroserviceDescriptor> appMsIterator = appMsList.iterator();
        while (appMsIterator.hasNext()) {
            FoDAppMicroserviceDescriptor currentMs = appMsIterator.next(); currentMs.setApplicationId(appId);
            if (isMicroserviceId && currentMs.getMicroserviceId().equals(microserviceId)) return currentMs;
            if (!isMicroserviceId && currentMs.getMicroserviceName().equals(appAndMicroserviceName[1])) return currentMs;
        }
        if (failIfNotFound)
            throw new ValidationException("No microservice found for application name/id and microservice name/id: " + appAndMicroserviceNameOrId);
        return null;
    }

    public static final JsonNode createAppMicroservice(UnirestInstance unirest, Integer appId, FoDAppMicroserviceUpdateRequest msRequest) {
        ObjectNode body = objectMapper.valueToTree(msRequest);
        JsonNode response = unirest.post(FoDUrls.MICROSERVICES)
                .routeParam("appId", String.valueOf(String.valueOf(appId)))
                .body(body).asObject(JsonNode.class).getBody();
        FoDAppMicroserviceDescriptor descriptor = getDescriptor(response);
        ObjectNode node = getObjectMapper().createObjectNode();
        node.put("applicationId", appId);
        node.put("applicationName", FoDAppHelper.getApp(unirest, String.valueOf(appId), true).getApplicationName());
        node.put("microserviceId", descriptor.getMicroserviceId());
        node.put("microserviceName", msRequest.getMicroserviceName());
        return node;
    }

    public static final JsonNode updateteAppMicroservice(UnirestInstance unirest, FoDAppMicroserviceDescriptor currentMs, FoDAppMicroserviceUpdateRequest msRequest) {
        ObjectNode body = objectMapper.valueToTree(msRequest);
        JsonNode response = unirest.put(FoDUrls.MICROSERVICES_UPDATE)
                .routeParam("appId", String.valueOf(currentMs.getApplicationId()))
                .routeParam("microserviceId", String.valueOf(currentMs.getMicroserviceId()))
                .body(body).asObject(JsonNode.class).getBody();
        FoDAppMicroserviceDescriptor descriptor = getDescriptor(response);
        ObjectNode node = getObjectMapper().createObjectNode();
        node.put("applicationId", currentMs.getApplicationId());
        node.put("applicationName", FoDAppHelper.getApp(unirest, String.valueOf(currentMs.getApplicationId()), true).getApplicationName());
        node.put("microserviceId", descriptor.getMicroserviceId());
        node.put("microserviceName", msRequest.getMicroserviceName());
        return node;
    }

    public static final JsonNode deleteAppMicroservice(UnirestInstance unirest, FoDAppMicroserviceDescriptor currentMs) {
        FoDAppDescriptor appDescriptor = FoDAppHelper.getApp(unirest, String.valueOf(currentMs.getApplicationId()), true);
        JsonNode response = unirest.delete(FoDUrls.MICROSERVICES_UPDATE)
                .routeParam("appId", String.valueOf(currentMs.getApplicationId()))
                .routeParam("microserviceId", String.valueOf(currentMs.getMicroserviceId()))
                .asObject(JsonNode.class).getBody();
        ObjectNode node = getObjectMapper().createObjectNode();
        node.put("applicationName", appDescriptor.getApplicationName());
        node.put("microserviceId", currentMs.getMicroserviceId());
        node.put("microserviceName", currentMs.getMicroserviceName());
        return node;
    }

    private static final FoDAppMicroserviceDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDAppMicroserviceDescriptor.class);
    }


}
