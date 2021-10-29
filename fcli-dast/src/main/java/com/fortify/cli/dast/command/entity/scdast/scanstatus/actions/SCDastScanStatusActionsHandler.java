package com.fortify.cli.dast.command.entity.scdast.scanstatus.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.dast.command.entity.scdast.scan.actions.SCDastScanActionsHandler;
import com.fortify.cli.dast.command.entity.types.ScanStatusTypes;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

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

        public JsonNode getScanStatus(int scanId) {
        JsonNode response = scanActionsHandler.getFilteredScanSummary(scanId, new String[]{"scanStatusType"});

        int scanStatusInt = Integer.parseInt(response.get("scanStatusType").toString());
        ((ObjectNode) response).put(
                "scanStatusTypeString",
                ScanStatusTypes.getStatusString(scanStatusInt -1).replace("\"",""));

        return response;
    }
}
