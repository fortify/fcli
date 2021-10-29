package com.fortify.cli.dast.command.crud.scdast.scan.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.JsonNodeFilterHandler;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ReflectiveAccess
@RequiredArgsConstructor
public class SCDastScanActionsHandler {
    @Getter @Inject @NonNull
    UnirestInstance unirest;


    public JsonNode getScanSummary(int scanId) {
        String urlPath = "/api/v2/scans/"+ scanId + "/scan-summary";
        return getUnirest().get(urlPath)
                .accept("application/json")
                .header("Content-Type", "application/json")
                .asObject(ObjectNode.class)
                .getBody()
                .get("item");
    }

    public JsonNode getFilteredScanSummary(int scanId, String[] fields) {
        JsonNode response = getScanSummary(scanId);

        Set<String> outputFields = new HashSet<>(Arrays.asList(fields));
        JsonNodeFilterHandler.filterJsonNode(response, outputFields);

        return response;
    }
}
