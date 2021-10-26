package com.fortify.cli.dast.command.entity.SCDast.ScanSettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.output.OutputWriterMixin;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;

public class SCDastScanSettingsByIdCommands {
    private static final String NAME = "with-id";
    private static final String DESC = "DAST scan settings";

    @ReflectiveAccess
    @SubcommandOf(SCDastScanSettingsCommands.Get.class)
    @CommandLine.Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand {

        @CommandLine.Parameters(description = "The scan settings id" )
        @Getter private String scanSettingsId;

        @CommandLine.Mixin
        private OutputWriterMixin outputWriterMixin;


        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/application-version-scan-settings/" + getScanSettingsId();

            JsonNode response = unirest.get(urlPath)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .asObject(ObjectNode.class)
                    .getBody();

            outputWriterMixin.printToFormat(response);

            return null;
        }
    }
}

