package com.fortify.cli.sc_dast.command.crud.scanresults.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.sc_dast.command.crud.scan.actions.SCDastScanActionsHandler;
import com.fortify.cli.sc_dast.command.crud.scanstatus.actions.SCDastScanStatusActionsHandler;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

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
        scanStatusActionsHandler.waitCompletion(scanId, waitInterval);

        return null;
    }

    @SneakyThrows
    public Void waitCompletionWithDetails(int scanId, int waitInterval) {
        scanStatusActionsHandler.waitCompletionWithDetails(scanId, waitInterval);

        return null;
    }
}
