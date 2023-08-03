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

package com.fortify.cli.fod.microservice.helper;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.app.helper.FoDAppDescriptor;
import com.fortify.cli.fod.app.helper.FoDAppHelper;

import kong.unirest.UnirestInstance;

public class FoDMicroserviceHelper {
    private static final ObjectMapper objectMapper = JsonHelper.getObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDMicroserviceDescriptor getMicroserviceDescriptor(UnirestInstance unirest, FoDQualifiedMicroserviceNameDescriptor microserviceNameDescriptor, boolean failIfNotFound) {
        var app = getAppDescriptor(unirest, microserviceNameDescriptor, failIfNotFound);
        // The call above can only return null if failIfNotFound==false, 
        // so we can simply return null if no app was found.
        if ( app==null ) { return null; }
        return getMicroserviceDescriptor(unirest, app, microserviceNameDescriptor, failIfNotFound);       
    }

    private static final FoDMicroserviceDescriptor getMicroserviceDescriptor(UnirestInstance unirest, FoDAppDescriptor appDescriptor,
            FoDQualifiedMicroserviceNameDescriptor microserviceNameDescriptor, boolean failIfNotFound) {
        var microservices = (ArrayNode)unirest.get(FoDUrls.MICROSERVICES).routeParam("appId", appDescriptor.getApplicationId())
            .asObject(JsonNode.class).getBody().get("items");
        var matching = JsonHelper.stream(microservices)
                .filter(match(microserviceNameDescriptor))
                .collect(Collectors.toList());
        switch ( matching.size() ) {
        case 0:
            return nullOrNotFoundException(microserviceNameDescriptor, failIfNotFound);
        case 1:
            return getDescriptor(appDescriptor, microservices.get(0));
        default:
            // FoD usually doesn't allow duplicate microservice names, but we handle this
            // by throwing an exception, just in case.
            throw multipleFoundException(microserviceNameDescriptor);
        }
    }

    private static final FoDAppDescriptor getAppDescriptor(UnirestInstance unirest, FoDQualifiedMicroserviceNameDescriptor microserviceNameDescriptor, boolean failIfNotFound) {
        var appName = microserviceNameDescriptor.getAppName();
        var app = FoDAppHelper.getAppDescriptor(unirest, appName, failIfNotFound);
        if ( app!=null && !app.isHasMicroservices() ) {
            throw new IllegalArgumentException("Cannot get microservice data from non-microservice application "+microserviceNameDescriptor.getAppName());
        }
        return app;
    }

    private static final Predicate<? super JsonNode> match(FoDQualifiedMicroserviceNameDescriptor microserviceNameDescriptor) {
        return ms->microserviceNameDescriptor.getMicroserviceName().equals(ms.get("microserviceName").asText());
    }
    
    private static final FoDMicroserviceDescriptor nullOrNotFoundException(FoDQualifiedMicroserviceNameDescriptor microserviceNameDescriptor, boolean failIfNotFound) {
        if ( failIfNotFound ) {
            throw new IllegalArgumentException(String.format("Cannot find microservice %s on application %s", microserviceNameDescriptor.getMicroserviceName(), microserviceNameDescriptor.getAppName()));
        }
        return null;
    }
    
    private static final RuntimeException multipleFoundException(FoDQualifiedMicroserviceNameDescriptor microserviceNameDescriptor) {
        return new IllegalStateException(String.format("Multiple microservices found with name %s on application %s", microserviceNameDescriptor.getMicroserviceName(), microserviceNameDescriptor.getAppName()));
    }

    public static final FoDMicroserviceDescriptor createMicroservice(UnirestInstance unirest, FoDAppDescriptor appDescriptor, FoDMicroserviceUpdateRequest msRequest) {
        ObjectNode body = objectMapper.valueToTree(msRequest);
        JsonNode response = unirest.post(FoDUrls.MICROSERVICES)
                .routeParam("appId", appDescriptor.getApplicationId())
                .body(body).asObject(JsonNode.class).getBody();
        return getDescriptor(appDescriptor, response, msRequest.getMicroserviceName());
    }

    public static final FoDMicroserviceDescriptor updateMicroservice(UnirestInstance unirest, FoDMicroserviceDescriptor currentMs, FoDMicroserviceUpdateRequest msRequest) {
        FoDAppDescriptor appDescriptor = FoDAppHelper.getAppDescriptor(unirest, currentMs.getApplicationId(), true);
        ObjectNode body = objectMapper.valueToTree(msRequest);
        JsonNode response = unirest.put(FoDUrls.MICROSERVICES_UPDATE)
                .routeParam("appId", currentMs.getApplicationId())
                .routeParam("microserviceId", currentMs.getMicroserviceId())
                .body(body).asObject(JsonNode.class).getBody();
        return getDescriptor(appDescriptor, response, msRequest.getMicroserviceName());
    }

    public static final FoDMicroserviceDescriptor deleteMicroservice(UnirestInstance unirest, FoDMicroserviceDescriptor currentMs) {
        unirest.delete(FoDUrls.MICROSERVICES_UPDATE)
                .routeParam("appId", currentMs.getApplicationId())
                .routeParam("microserviceId", currentMs.getMicroserviceId())
                .asObject(JsonNode.class).getBody();
        return currentMs;
    }
    
    private static final FoDMicroserviceDescriptor getDescriptor(FoDAppDescriptor appDescriptor, JsonNode responseWithIdOnly, String microserviceName) {
        if ( responseWithIdOnly instanceof ObjectNode ) {
            return getDescriptor(appDescriptor, (ObjectNode)responseWithIdOnly, microserviceName);
        } else {
            throw new RuntimeException("Expected ObjectNode, got "+responseWithIdOnly.getClass().getSimpleName());
        }
    }

    private static final FoDMicroserviceDescriptor getDescriptor(FoDAppDescriptor appDescriptor, ObjectNode responseWithIdOnly, String microserviceName) {
        return getDescriptor(appDescriptor, responseWithIdOnly.put("microserviceName", microserviceName));
    }
    
    private static final FoDMicroserviceDescriptor getDescriptor(FoDAppDescriptor appDescriptor, JsonNode node) {
        if ( node instanceof ObjectNode ) {
            return getDescriptor(appDescriptor, (ObjectNode)node);
        } else {
            throw new RuntimeException("Expected ObjectNode, got "+node.getClass().getSimpleName());
        }
    }
    
    private static final FoDMicroserviceDescriptor getDescriptor(FoDAppDescriptor appDescriptor, ObjectNode node) {
        var fullNode = node.deepCopy()
                .put("applicationId", appDescriptor.getApplicationId())
                .put("applicationName", appDescriptor.getApplicationName());
        return JsonHelper.treeToValue(fullNode, FoDMicroserviceDescriptor.class);
    }
}
