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
package com.fortify.cli.sc_dast.entity.sensor.helper;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.sc_dast.rest.helper.SCDastInputTransformer;
import com.fortify.cli.sc_dast.rest.helper.SCDastPagingHelper;

import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;

public class SCDastSensorHelper {
    private SCDastSensorHelper() {}
    
    public static final SCDastSensorDescriptor getSensorDescriptor(UnirestInstance unirest, String sensorNameOrId) {
        try {
            int sensorId = Integer.parseInt(sensorNameOrId);
            JsonNode sensorNode = unirest.get(String.format("/api/v2/scanners/%s",sensorId)).asObject(JsonNode.class).getBody();
            return getDescriptor(sensorNode);
        } catch (NumberFormatException nfe) {
            List<JsonNode> matchingSensors = SCDastPagingHelper.pagedRequest(unirest.get("/api/v2/scanners")).stream()
                .map(HttpResponse::getBody)
                .map(SCDastInputTransformer::getItems)
                .map(ArrayNode.class::cast)
                .flatMap(JsonHelper::stream)
                .filter(j -> j.get("name").asText().equals(sensorNameOrId)) // TODO Add null checks?
                .collect(Collectors.toList());
            if ( matchingSensors.isEmpty() ) {
                throw new IllegalArgumentException("No sensor found with name "+sensorNameOrId);
            } else if ( matchingSensors.size()>1 ) {
                throw new IllegalArgumentException("Multiple sensors found with name "+sensorNameOrId);
            } else {
                return getDescriptor(matchingSensors.get(0));
            }
        }
    }

    private static final SCDastSensorDescriptor getDescriptor(JsonNode sensorNode) {
        return JsonHelper.treeToValue(sensorNode, SCDastSensorDescriptor.class);
    }

    public static final SCDastSensorDescriptor enableSensor(UnirestInstance unirest, SCDastSensorDescriptor descriptor) {
        return changeSensorState(unirest, descriptor, true);
    }
    
    public static final SCDastSensorDescriptor disableSensor(UnirestInstance unirest, SCDastSensorDescriptor descriptor) {
        return changeSensorState(unirest, descriptor, false);
    }

    private static final SCDastSensorDescriptor changeSensorState(UnirestInstance unirest, SCDastSensorDescriptor descriptor, boolean enable) {
        if ( enable==descriptor.isEnabled() ) {
            return descriptor;
        } else {
            ObjectNode data = new ObjectMapper().createObjectNode().put("isEnabled", enable);
            unirest.post("/api/v2/scanners/{id}/set-scanner-enabled-value")
            	.routeParam("id", descriptor.getId())
            	.body(data).asString().getBody();
            return getSensorDescriptor(unirest, descriptor.getId());
        }
    }
}
