package com.fortify.cli.sc_dast.picocli.command.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import java.io.File;

@ReflectiveAccess
public class SCDastTransferScanLogsActionsHandler {
    @Getter @Inject
    UnirestInstance unirest;

    public SCDastTransferScanLogsActionsHandler(UnirestInstance unirest) {
        this.unirest = unirest;
    }

    public ObjectNode downloadScanLogs(int scanId, File file) {
        String urlPath = "/api/v2/scans/"+ scanId + "/download-logs";
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
