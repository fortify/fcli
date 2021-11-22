package com.fortify.cli.sc_dast.picocli.command.util;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

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
