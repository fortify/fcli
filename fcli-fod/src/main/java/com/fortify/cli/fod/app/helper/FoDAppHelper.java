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
package com.fortify.cli.fod.app.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupDescriptor;
import com.fortify.cli.fod.user_group.helper.FoDUserGroupHelper;
import io.micronaut.core.util.StringUtils;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;

public class FoDAppHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[] {
                "applicationId:id", "applicationName:name", "applicationDescription:description",
                "businessCriticalityType:criticality", "applicationType:type"
        }).transform(record);
    }

    public static final FoDAppDescriptor getApp(UnirestInstance unirest, String appNameOrId, boolean failIfNotFound) {
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
            if (StringUtils.isEmpty(appCreateRequest.getReleaseMicroserviceName()))
                body.replace("releaseMicroserviceName", appCreateRequest.getMicroservices()).get(0).asText();
        }
        JsonNode response = unirest.post(FoDUrls.APPLICATIONS)
                .body(body).asObject(JsonNode.class).getBody();
        FoDAppDescriptor descriptor = JsonHelper.treeToValue(response, FoDAppDescriptor.class);
        descriptor.asObjectNode()
                .put("applicationName", appCreateRequest.getApplicationName())
                .put("releaseName", appCreateRequest.getReleaseName());
        return descriptor;
    }

    public static final FoDAppDescriptor updateApp(UnirestInstance unirest, Integer appId,
                                                   FoDAppUpdateRequest appUpdateRequest) {
        ObjectNode body = objectMapper.valueToTree(appUpdateRequest);
        unirest.put(FoDUrls.APPLICATION)
                .routeParam("appId", String.valueOf(appId))
                .body(body).asObject(JsonNode.class).getBody();
        return getApp(unirest, String.valueOf(appId), true);
    }

    public static String getEmailList(ArrayList<String> notifications) {
        if (notifications != null && !notifications.isEmpty()) {
            return String.join(",", notifications);
        } else {
            return "";
        }
    }

    public static JsonNode getMicroservicesNode(ArrayList<String> microservices) {
        ArrayNode microserviceArray = objectMapper.createArrayNode();
        if (microservices == null || microservices.isEmpty()) return microserviceArray;
        for (String ms : microservices) {
            microserviceArray.add(ms);
        }
        return microserviceArray;
    }

    public static JsonNode getUserGroupsNode(ArrayList<Integer> userGroups) {
        ArrayNode userGroupArray = objectMapper.createArrayNode();
        if (userGroups == null || userGroups.isEmpty()) return userGroupArray;
        for (Integer ug : userGroups) {
            userGroupArray.add(ug);
        }
        return userGroupArray;
    }

    public static JsonNode getUserGroupsNode(UnirestInstance unirest, ArrayList<String> userGroups) {
        ArrayNode userGroupArray = getObjectMapper().createArrayNode();
        if (userGroups == null || userGroups.isEmpty()) return userGroupArray;
        for (String ug : userGroups) {
            FoDUserGroupDescriptor userGroupDescriptor = FoDUserGroupHelper.getUserGroup(unirest, ug, true);
            userGroupArray.add(userGroupDescriptor.getId());
        }
        return userGroupArray;
    }

    public static boolean missing(List<?> list) {
        return list == null || list.isEmpty();
    }

    private static final FoDAppDescriptor getDescriptor(JsonNode node) {
        return  JsonHelper.treeToValue(node, FoDAppDescriptor.class);
    }


}
