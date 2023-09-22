/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_dast.scan.cli.cmd;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.util.EnvSuffix;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.sc_dast._common.output.cli.cmd.AbstractSCDastOutputCommand;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanHelper;
import com.fortify.cli.sc_dast.scan_policy.cli.mixin.SCDastScanPolicyResolverMixin;
import com.fortify.cli.sc_dast.scan_settings.cli.mixin.SCDastScanSettingsResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = OutputHelperMixins.Start.CMD_NAME)
public final class SCDastScanStartCommand extends AbstractSCDastOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Start outputHelper;
    @EnvSuffix("NAME") @Parameters(index = "0", arity = "1", paramLabel = "name") private String scanName;
    @Mixin private SCDastScanSettingsResolverMixin.RequiredOption scanSettingsResolver;
    @Mixin private SCDastScanPolicyResolverMixin.OptionalOption scanPolicyResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Option(names = "--overrides-file") private File overridesFile;

    private enum ScanModes {CrawlOnly, CrawlAndAudit, AuditOnly}
    @Option(names= {"--mode", "-m"})
    private ScanModes scanMode;

    @Option(names = {"--login-macro", "-l"})
    private Integer loginMacroBinaryFileId;
    
    @Override
    public JsonNode getJsonNode() {
        var unirest = getUnirestInstance();
        String scanId = unirest.post("/api/v2/scans/start-scan-cicd")
            .accept("application/json")
            .header("Content-Type", "application/json")
            .body(getBody(unirest))
            .asObject(JsonNode.class)
            .getBody().get("id").asText();
        return SCDastScanHelper.getScanDescriptor(unirest, scanId).asJsonNode();
    }
    
    @Override
    public String getActionCommandResult() {
        return "START_REQUESTED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
        
    private ObjectNode getBody(UnirestInstance unirest) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", scanName); 
        body.put("cicdToken", scanSettingsResolver.getScanSettingsCicdToken(unirest));
        body.set("overrides", getOverrides(unirest));

        return body;
    }

    private ObjectNode getOverrides(UnirestInstance unirest) {
        ObjectNode overridesJson = objectMapper.createObjectNode();
        if ( overridesFile!=null) {
            try {
                overridesJson = objectMapper.readValue(overridesFile, ObjectNode.class);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to read "+overridesFile+" as valid JSON", e);
            }
        }
        if ( scanMode!=null ) { overridesJson.put("scanMode", scanMode.name()); }
        String scanPolicyId = scanPolicyResolver.getScanPolicyId(unirest);
        if ( scanPolicyId!=null ) { overridesJson.put("policyId", scanPolicyId); }
        if ( loginMacroBinaryFileId!=null ) overridesJson.put("loginMacroBinaryFileId", loginMacroBinaryFileId);
        return overridesJson;
    }
}