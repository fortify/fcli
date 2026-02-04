/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.sc_sast.sensor.helper;

import java.util.Comparator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.util.SemVer;

import kong.unirest.UnirestInstance;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Helper class for determining ScanCentral SAST client compatible versions from sensor data.
 * Uses Lombok @Builder to configure SSC UnirestInstance and optional pool/app version filters.
 * Provides methods to retrieve and process sensor version data.
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SCSastSensorCompatibleVersionHelper {
    private final UnirestInstance unirest;
    private final String poolUuid;
    private final String appVersionId;
    
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final JsonNode sensorData = fetchSensorData();
    
    /**
     * Stream all compatible versions sorted in descending order (newest first).
     * 
     * @return Stream of compatible version information as ObjectNodes
     */
    public Stream<ObjectNode> streamCompatibleVersions() {
        return StreamSupport.stream(getSensorData().spliterator(), false)
            .filter(this::isActiveSensor)
            .map(sensor -> sensor.get("scaVersion"))
            .filter(v -> v != null && !v.isNull())
            .map(JsonNode::asText)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .map(SemVer::new)
            .filter(SemVer::isProperSemver)
            .sorted(Comparator.reverseOrder())
            .map(version -> {
                ObjectNode node = JsonHelper.getObjectMapper().createObjectNode();
                node.put("sensorVersion", version.getSemver());
                node.put("compatibleClientVersion", version.getMajorMinor().orElse(""));
                return node;
            });
    }
    
    /**
     * Get the latest compatible client version.
     * 
     * @return Latest compatible client version string (e.g., "24.4")
     * @throws FcliSimpleException if no compatible versions found
     */
    public String getLatestCompatibleVersion() {
        return streamCompatibleVersions()
            .findFirst()
            .map(node -> node.get("compatibleClientVersion").asText())
            .orElseThrow(() -> new FcliSimpleException(
                poolUuid != null 
                    ? "No active sensors found in the specified sensor pool"
                    : appVersionId != null
                        ? "No active sensors found for the application version's sensor pool"
                        : "No active sensors found in any sensor pool"
            ));
    }
    
    private boolean isActiveSensor(JsonNode sensor) {
        JsonNode state = sensor.get("state");
        return state != null && "ACTIVE".equalsIgnoreCase(state.asText());
    }
    
    private JsonNode fetchSensorData() {
        validateMutualExclusivity();
        String effectivePoolUuid = determinePoolUuid();
        
        if (effectivePoolUuid != null) {
            return unirest.get("/api/v1/cloudpools/{uuid}/workers")
                .routeParam("uuid", effectivePoolUuid)
                .asObject(ObjectNode.class)
                .getBody()
                .get("data");
        }
        
        return unirest.get("/api/v1/cloudworkers")
            .asObject(ObjectNode.class)
            .getBody()
            .get("data");
    }
    
    private void validateMutualExclusivity() {
        if (poolUuid != null && appVersionId != null) {
            throw new FcliSimpleException("Cannot specify both pool and app version");
        }
    }
    
    private String determinePoolUuid() {
        if (appVersionId != null) {
            return mapAppVersionToPool();
        }
        return poolUuid;
    }
    
    private String mapAppVersionToPool() {
        JsonNode response = unirest.get("/api/v1/cloudmappings/mapByVersionId")
            .queryString("projectVersionId", appVersionId)
            .asObject(ObjectNode.class)
            .getBody();
        
        JsonNode cloudPool = response.path("data").path("cloudPool");
        if (cloudPool.isMissingNode() || cloudPool.isNull()) {
            throw new FcliSimpleException("Application version is not mapped to any sensor pool");
        }
        
        return cloudPool.path("uuid").asText();
    }
}
