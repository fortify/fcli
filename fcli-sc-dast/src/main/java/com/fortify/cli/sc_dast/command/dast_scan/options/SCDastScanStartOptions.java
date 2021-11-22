package com.fortify.cli.sc_dast.command.dast_scan.options;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;

/**
 * Configure options for running SC DAST Scan
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastScanStartOptions {

    @Option(names = {"-s", "--settings","--settings-id","--settings-identifier"}, description = "The Settings Identifier to run the scan with.", required = true)
    @Getter private String settingsId;

    @Option(names = {"-n","--scan-name"}, description = "The name of the SC DAST scan")
    @Getter private String scanName;

    @Option(names = {"--overrides"}, description = "File containing override valuse for the SC DAST scan")
    @Getter private File overridesFile;

    private enum ScanModes {CrawlOnly, CrawlAndAudit, AuditOnly}
    @Option(names= {"-M", "--mode", "--scan-mode"}, description = "Overrides the scan mode.")
    @Getter private ScanModes scanMode;

    @Option(names = {"-U", "--url","--start-url"}, description = "Overrides the scan start URL")
    @Getter private List<String> startUrls;

    @Option(names = {"-P", "--policy","--policy-id"}, description = "Overrides the scan policy id")
    @Getter private String policyId;

    @Option(names = {"-L","--login-macro"}, description = "Overrides the scan login macro binary file id")
    @Getter private Integer loginMacroBinaryFileId;

    @SneakyThrows
    public String getJsonBody() {
        JsonMapper jsonMapper = new JsonMapper();
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode body = objectMapper.createObjectNode();
        body.set("cicdToken", objectMapper.convertValue(getSettingsId(), JsonNode.class));

        ObjectNode overridesJson = objectMapper.createObjectNode();

        if (getScanName() != null) {
            body.set("name", objectMapper.convertValue(getScanName(), JsonNode.class));
        }
        if (getOverridesFile() != null) {
            overridesJson = jsonMapper.readValue(getOverridesFile(), ObjectNode.class);
        } else {
            if (getScanMode() != null) overridesJson.put("scanMode", getScanMode().toString());
            if (getStartUrls() != null) overridesJson.putArray("startUrls").addAll((ArrayNode) jsonMapper.valueToTree(getStartUrls()));
            if (getPolicyId() != null) overridesJson.put("policyId", getPolicyId());
            if (getLoginMacroBinaryFileId() != null) overridesJson.put("loginMacroBinaryFileId", getLoginMacroBinaryFileId());
        }

        body.set("overrides", overridesJson);

        return  jsonMapper.writeValueAsString(body);
    }
}
