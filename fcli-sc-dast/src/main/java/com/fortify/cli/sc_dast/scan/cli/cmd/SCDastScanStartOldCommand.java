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
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.sc_dast.rest.cli.cmd.AbstractSCDastUnirestRunnerCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@ReflectiveAccess
@Command(name = "start")
public final class SCDastScanStartOldCommand extends AbstractSCDastUnirestRunnerCommand {
    @Spec CommandSpec spec;

    @ArgGroup(exclusive = false, headingKey = "arggroup.start-scan-options.heading", order = 1)
    @Getter private SCDastScanStartOptions scanOptions;

    @Mixin private OutputMixin outputMixin;
    
    @ReflectiveAccess
    public static class SCDastScanStartOptions {

        @Option(names = {"-s", "--settings","--settings-id","--settings-identifier"}, required = true)
        @Getter private String settingsId;

        @Option(names = {"-n","--scan-name"})
        @Getter private String scanName;

        @Option(names = {"--overrides"})
        @Getter private File overridesFile;

        private enum ScanModes {CrawlOnly, CrawlAndAudit, AuditOnly}
        @Option(names= {"-M", "--mode", "--scan-mode"})
        @Getter private ScanModes scanMode;

        @Option(names = {"-U", "--url","--start-url"})
        @Getter private List<String> startUrls;

        @Option(names = {"-P", "--policy","--policy-id"})
        @Getter private String policyId;

        @Option(names = {"-L","--login-macro"})
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

    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        if(scanOptions == null){
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Error: No parameter found. Provide the required scan-settings identifier.");
        }
        outputMixin.write(unirest.post("/api/v2/scans/start-scan-cicd")
                .accept("application/json")
                .header("Content-Type", "application/json")
                .body(scanOptions.getJsonBody())
                .asObject(ObjectNode.class));
        return null;
    }
    
    
}