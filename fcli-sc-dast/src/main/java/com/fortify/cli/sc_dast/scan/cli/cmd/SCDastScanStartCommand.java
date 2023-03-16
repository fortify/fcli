/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.sc_dast.scan.cli.cmd;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.sc_dast.output.cli.cmd.AbstractSCDastOutputCommand;
import com.fortify.cli.sc_dast.output.cli.mixin.SCDastOutputHelperMixins;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanHelper;
import com.fortify.cli.sc_dast.scan_policy.cli.mixin.SCDastScanPolicyResolverMixin;
import com.fortify.cli.sc_dast.scan_settings.cli.mixin.SCDastScanSettingsResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = SCDastOutputHelperMixins.Start.CMD_NAME)
public final class SCDastScanStartCommand extends AbstractSCDastOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SCDastOutputHelperMixins.Start outputHelper;
    @Parameters(index = "0", arity = "1", paramLabel = "name") private String scanName;
    @Mixin private SCDastScanSettingsResolverMixin.RequiredOption scanSettingsResolver;
    @Mixin private SCDastScanPolicyResolverMixin.OptionalOption scanPolicyResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Option(names = "--overrides-file") private File overridesFile;

    private enum ScanModes {CrawlOnly, CrawlAndAudit, AuditOnly}
    @Option(names= {"--mode", "-M"})
    private ScanModes scanMode;

    @Option(names = {"--start-url", "-U"})
    private String[] startUrls;

    @Option(names = {"--login-macro", "-L"})
    private Integer loginMacroBinaryFileId;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
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
        if ( startUrls!=null && startUrls.length>0) { overridesJson.set("startUrls", JsonHelper.toArrayNode(startUrls)); }
        String scanPolicyId = scanPolicyResolver.getScanPolicyId(unirest);
        if ( scanPolicyId!=null ) { overridesJson.put("policyId", scanPolicyId); }
        if ( loginMacroBinaryFileId!=null ) overridesJson.put("loginMacroBinaryFileId", loginMacroBinaryFileId);
        return overridesJson;
    }
}