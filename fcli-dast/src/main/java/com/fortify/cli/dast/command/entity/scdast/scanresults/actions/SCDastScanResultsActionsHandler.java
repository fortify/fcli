package com.fortify.cli.dast.command.entity.scdast.scanresults.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.JsonNodeFilterHandler;
import com.fortify.cli.dast.command.entity.scdast.scan.actions.SCDastScanActionsHandler;
import com.fortify.cli.dast.command.entity.scdast.scanstatus.actions.SCDastScanStatusActionsHandler;
import com.fortify.cli.dast.command.entity.types.ScanStatusTypes;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@ReflectiveAccess
@AllArgsConstructor
public class SCDastScanResultsActionsHandler {
    @Getter @Inject
    UnirestInstance unirest;
    @Getter
    SCDastScanActionsHandler scanActionsHandler;
    @Getter
    SCDastScanStatusActionsHandler scanStatusActionsHandler;

    public SCDastScanResultsActionsHandler(UnirestInstance unirest) {
        this.unirest = unirest;
        this.scanActionsHandler = new SCDastScanActionsHandler(unirest);
        this.scanStatusActionsHandler = new SCDastScanStatusActionsHandler(unirest);
    }

    public JsonNode getScanResults(int scanId) {
        String[] fields = new String[]{"lowCount", "mediumCount", "highCount", "criticalCount"};
        return scanActionsHandler.getFilteredScanSummary(scanId, fields);
    }

    @SneakyThrows
    public Void waitCompletion(int scanId, int waitInterval) {
        List<String> waitingStatus = Arrays.asList("Pending", "Queued", "Running");
        String scanStatus =  scanStatusActionsHandler.getScanStatus(scanId).get("scanStatusTypeString")
                .toString()
                .replace("\"","");
        int i = 0;

        while (waitingStatus.contains(scanStatus)) {
            System.out.println(i + ") Scan status: "+scanStatus);
            TimeUnit.SECONDS.sleep(waitInterval);
            scanStatus =  scanStatusActionsHandler.getScanStatus(scanId).get("scanStatusTypeString")
                    .toString()
                    .replace("\"","");
            i += 1;
        }

        System.out.println(i + ") Scan status: "+scanStatus);

        return null;
    }
}
