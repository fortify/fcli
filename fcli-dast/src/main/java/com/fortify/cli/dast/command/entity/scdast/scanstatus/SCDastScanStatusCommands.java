package com.fortify.cli.dast.command.entity.scdast.scanstatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.output.OutputWriterMixin;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.entity.types.ScanStatusTypes;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

public class SCDastScanStatusCommands {
    private static final String NAME = "scan-status";
    private static final String DESC = "DAST scan status";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand {
        @Option(names = {"-id", "--scan-id"}, description = "The scan id")
        @Getter private String scanId;

        @Mixin
        private OutputWriterMixin outputWriterMixin;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/scans/"+ getScanId() + "/scan-summary";

            JsonNode response = unirest.get(urlPath)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .asObject(ObjectNode.class)
                    .getBody()
                    .get("item")
                    .get("scanStatusType");

            String status = ScanStatusTypes.values()[Integer.parseInt(response.asText()) - 1].toString();

            System.out.println(status);

            return null;
        }
    }
}

