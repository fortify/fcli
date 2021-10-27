package com.fortify.cli.dast.command.entity.scdast.scan.options;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;

import java.io.File;

/**
 * Configure options for running SC DAST Scan
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastScanOptions {

    @Option(names = {"-settings","--settings-id","--settings-identifier"}, description = "The Settings Identifier to run the scan with.", required = true)
    @Getter private String settingsId;

    @Option(names = {"-n","--scan-name"}, description = "The name of the SC DAST scan")
    @Getter private String scanName;

    @Option(names = {"--overrides"}, description = "File containing override valuse for the SC DAST scan")
    @Getter private File overridesFile;

    @SneakyThrows
    public String getJsonBody() {
        JsonMapper jsonMapper = new JsonMapper();
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode body = objectMapper.createObjectNode();
        body.set("cicdToken", objectMapper.convertValue(getSettingsId(), JsonNode.class));

        if (getScanName() != null) {
            body.set("name", objectMapper.convertValue(getScanName(), JsonNode.class));
        }
        if (getOverridesFile() != null) {
            JsonNode overridesJson = jsonMapper.readValue(getOverridesFile(), JsonNode.class);
            body.set("overrides", overridesJson);
        }

        return  jsonMapper.writeValueAsString(body);
    }
}
