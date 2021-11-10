package com.fortify.cli.sc_dast.command.transfer.scanresults.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.sc_dast.command.crud.scan.actions.SCDastScanActionsHandler;
import com.fortify.cli.sc_dast.command.crud.scanstatus.actions.SCDastScanStatusActionsHandler;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;

@ReflectiveAccess
public class SCDastTransferScanResultsActionsHandler {
    @Getter @Inject
    UnirestInstance unirest;

    public SCDastTransferScanResultsActionsHandler(UnirestInstance unirest) {
        this.unirest = unirest;
    }

    public ObjectNode downloadScanResults(int scanId, File file) {
        String urlPath = "/api/v2/scans/"+ scanId + "/download-results";
        getUnirest().get(urlPath)
                .accept("application/json")
                .header("Content-Type", "application/json")
                .asFile(file.getPath())
                .getBody();

        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("path", file.getPath());

        return output;
    }
}
