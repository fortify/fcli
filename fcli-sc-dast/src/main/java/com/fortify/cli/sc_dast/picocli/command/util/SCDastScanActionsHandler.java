package com.fortify.cli.sc_dast.picocli.command.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JacksonJsonNodeHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.NonNull;
import lombok.SneakyThrows;

// TODO Review this class, refactor as needed, move to appropriate package, ...
@ReflectiveAccess
public class SCDastScanActionsHandler {
    @NonNull private final UnirestInstance unirest;

    public SCDastScanActionsHandler(UnirestInstance unirest) {
        this.unirest = unirest;
    }

    public JsonNode getScanSummary(int scanId) {
        String urlPath = "/api/v2/scans/"+ scanId + "/scan-summary";
        return unirest.get(urlPath)
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

    private JsonNode runScanAction(int scanId, String action){
        String urlPath = "/api/v2/scans/"+ scanId + "/scan-action";

        return unirest.post(urlPath)
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

    public JsonNode getScanResults(int scanId) {
        String[] fields = new String[]{"lowCount", "mediumCount", "highCount", "criticalCount"};
        return getFilteredScanSummary(scanId, fields);
    }
    
    public JsonNode getScanStatus(int scanId) {
        JsonNode response = getFilteredScanSummary(scanId, new String[]{"scanStatusType"});

        int scanStatusInt = Integer.parseInt(response.get("scanStatusType").toString());
        ((ObjectNode) response).put(
                "scanStatusTypeString",
                ScanStatusTypes.getStatusString(scanStatusInt).replace("\"",""));

        return response;
    }

    @SneakyThrows
    private void waitWhileScanStatus(int scanId, List<String> waitingStatus, int waitInterval) {
        String scanStatus =  getScanStatus(scanId).get("scanStatusTypeString")
                .toString()
                .replace("\"","");
        int i = 0;

        while (waitingStatus.contains(scanStatus)) {
            System.out.println(i + ") Scan status: "+scanStatus);
            TimeUnit.SECONDS.sleep(waitInterval);
            scanStatus =  getScanStatus(scanId).get("scanStatusTypeString")
                    .toString()
                    .replace("\"","");
            i += 1;
        }

        System.out.println(i + ") Scan status: "+scanStatus);
    }

    @SneakyThrows
    private void waitUntilScanStatus(int scanId, List<String> waitingStatus, int waitInterval) {
        String scanStatus =  getScanStatus(scanId).get("scanStatusTypeString")
                .toString()
                .replace("\"","");
        int i = 0;

        while (! waitingStatus.contains(scanStatus)) {
            System.out.println(i + ") Scan status: "+scanStatus);
            TimeUnit.SECONDS.sleep(waitInterval);
            scanStatus =  getScanStatus(scanId).get("scanStatusTypeString")
                    .toString()
                    .replace("\"","");
            i += 1;
        }

        System.out.println(i + ") Scan status: "+scanStatus);
    }

    public void waitCompletion(int scanId, int waitInterval)
    {
        List<String> waitingStatus = Arrays.asList("Pending", "Queued", "Running");

        waitWhileScanStatus(scanId, waitingStatus, waitInterval);
    }

    public void waitCompletionWithDetails(int scanId, int waitInterval)
    {
        List<String> waitingStatus = Arrays.asList("Pending", "Queued", "Running");

        waitWhileScanStatus(scanId, waitingStatus, waitInterval);
    }

    public void waitPaused(int scanId, int waitInterval)
    {
        List<String> waitingStatus = Arrays.asList("Paused","Unknown");

        waitUntilScanStatus(scanId, waitingStatus, waitInterval);
    }

    public void waitResumed(int scanId, int waitInterval)
    {
        List<String> waitingStatus = Arrays.asList("Running","FailedToResume","FailedToStart","LicenseUnavailable","Unknown");

        waitUntilScanStatus(scanId, waitingStatus, waitInterval);
    }

    public void waitCompleted(int scanId, int waitInterval)
    {
        List<String> waitingStatus = Arrays.asList("Complete","ForcedComplete","LicenseUnavailable","Unknown");

        waitUntilScanStatus(scanId, waitingStatus, waitInterval);
    }

}
