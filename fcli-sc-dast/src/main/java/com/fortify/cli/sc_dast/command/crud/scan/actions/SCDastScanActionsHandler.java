package com.fortify.cli.sc_dast.command.crud.scan.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JacksonJsonNodeHelper;
import com.fortify.cli.sc_dast.command.crud.scanstatus.actions.SCDastScanStatusActionsHandler;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ReflectiveAccess
public class SCDastScanActionsHandler {
    @Getter @Inject @NonNull
    UnirestInstance unirest;
    @Getter
    SCDastScanStatusActionsHandler scanStatusActionsHandler;

    public SCDastScanActionsHandler(UnirestInstance unirest) {
        this.unirest = unirest;
        this.scanStatusActionsHandler = new SCDastScanStatusActionsHandler(unirest, this);
    }

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
        JacksonJsonNodeHelper.filterJsonNode(response, outputFields);

        return response;
    }

    public JsonNode startScan(String body){
        String urlPath = "/api/v2/scans/start-scan-cicd";

        return getUnirest().post(urlPath)
                .accept("application/json")
                .header("Content-Type", "application/json")
                .body(body)
                .asObject(ObjectNode.class)
                .getBody();
    }

    private JsonNode runScanAction(int scanId, String action){
        String urlPath = "/api/v2/scans/"+ scanId + "/scan-action";

        return getUnirest().post(urlPath)
                .header("Content-Type", "application/json")
                .body("{\"scanActionType\": \"" + action + "\"}")
                .asObject(ObjectNode.class)
                .getBody();
    }

    public JsonNode pauseScan(int scanId) {
        return runScanAction(scanId, "PauseScan");
    }

    public JsonNode resumeScan(int scanId) {
        return runScanAction(scanId, "ResumeScan");
    }

    public JsonNode completeScan(int scanId) {
        return runScanAction(scanId, "CompleteScan ");
    }

    public JsonNode deleteScan(int scanId) {
        return runScanAction(scanId, "DeleteScan ");
    }

    public JsonNode publishScan(int scanId) {
        return runScanAction(scanId, "RetryImportScanResults ");
    }

    public void waitPaused(int scanId, int waitInterval) { scanStatusActionsHandler.waitPaused(scanId, waitInterval); }

    public void waitResumed(int scanId, int waitInterval) { scanStatusActionsHandler.waitResumed(scanId, waitInterval); }

    public void waitCompleted(int scanId, int waitInterval) { scanStatusActionsHandler.waitCompleted(scanId, waitInterval); }


}
