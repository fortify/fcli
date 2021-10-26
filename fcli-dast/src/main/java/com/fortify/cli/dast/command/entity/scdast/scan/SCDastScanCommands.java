package com.fortify.cli.dast.command.entity.scdast.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.output.OutputWriterMixin;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.entity.scdast.scansettings.SCDastScanSettingsOptions;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpResponse;
import kong.unirest.JsonResponse;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;

public class SCDastScanCommands {
    private static final String NAME = "scan";
    private static final String DESC = "DAST scan";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTScanCommand.class)
    @CommandLine.Command(name = NAME, description = "Start " + DESC + " using SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand {

        @CommandLine.ArgGroup(exclusive = false, heading = "Scan options:%n", order = 1)
        @Getter
        private SCDastScanOptions scanOptions;

        @CommandLine.Mixin
        private OutputWriterMixin outputWriterMixin;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            JsonMapper jsonMapper = new JsonMapper();
            ObjectMapper objectMapper = new ObjectMapper();
            String urlPath = "/api/v2/scans/start-scan-cicd";

            ObjectNode body = objectMapper.createObjectNode();
            body.set("cicdToken", objectMapper.convertValue(getScanOptions().getCicdToken(), JsonNode.class));
            if(getScanOptions() != null) {
                if (getScanOptions().getScanName() != null) {
                    body.set("name", objectMapper.convertValue(getScanOptions().getScanName(), JsonNode.class));
                }
            }

            JsonNode response = unirest.post(urlPath)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .body(jsonMapper.writeValueAsString(body))
                    .asObject(ObjectNode.class)
                    .getBody();

            outputWriterMixin.printToFormat(response);

            return null;
        }
    }
}
