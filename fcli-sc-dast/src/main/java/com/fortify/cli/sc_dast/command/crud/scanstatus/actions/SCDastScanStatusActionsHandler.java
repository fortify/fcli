package com.fortify.cli.sc_dast.command.crud.scanstatus.actions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.sc_dast.command.crud.scan.actions.SCDastScanActionsHandler;
import com.fortify.cli.sc_dast.command.crud.scanstatus.types.ScanStatusTypes;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;

@ReflectiveAccess
public class SCDastScanStatusActionsHandler {
    @Getter @Inject
    UnirestInstance unirest;
    @Getter
    SCDastScanActionsHandler scanActionsHandler;

    public SCDastScanStatusActionsHandler(UnirestInstance unirest) {
        this.unirest = unirest;
        this.scanActionsHandler = new SCDastScanActionsHandler(unirest);
    }

    public SCDastScanStatusActionsHandler(UnirestInstance unirest,SCDastScanActionsHandler scanActionsHandler) {
        this.unirest = unirest;
        this.scanActionsHandler = scanActionsHandler;
    }

    public JsonNode getScanStatus(int scanId) {
        JsonNode response = scanActionsHandler.getFilteredScanSummary(scanId, new String[]{"scanStatusType"});

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
