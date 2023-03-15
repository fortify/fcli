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
package com.fortify.cli.sc_dast.sensor.helper;

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
