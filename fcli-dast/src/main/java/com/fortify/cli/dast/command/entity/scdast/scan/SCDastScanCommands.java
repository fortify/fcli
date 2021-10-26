package com.fortify.cli.dast.command.entity.scdast.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.FCLIRootCommand;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.output.OutputWriterMixin;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.entity.scdast.scan.ScanStatusTypes;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Command;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;


public class SCDastScanCommands {
    private static final String NAME = "scan";
    private static final String DESC = "DAST scan";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand {
        @Spec CommandSpec spec;

        @Option(names = {"-id", "--scan-id"}, description = "The scan id")
        @Getter private String scanId;

        @Mixin
        private OutputWriterMixin outputWriterMixin;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            if (getScanId() == null) {throw new CommandLine.ParameterException(spec.commandLine(),"Missing Scan Id");}

            String urlPath = "/api/v2/scans/"+ getScanId() + "/scan-summary";

            JsonNode response = unirest.get(urlPath)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .asObject(ObjectNode.class)
                    .getBody()
                    .get("item");

            outputWriterMixin.printToFormat(response);

            return null;
        }

    }

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTScanCommand.class)
    @Command(name = NAME, description = "Start " + DESC + " using SC DAST")
    public static final class Scan extends AbstractSCDastUnirestRunnerCommand {

        @ArgGroup(exclusive = false, heading = "Scan options:%n", order = 1)
        @Getter
        private SCDastScanOptions scanOptions;

        @Mixin
        private OutputWriterMixin outputWriterMixin;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/scans/start-scan-cicd";
            String body = scanOptions.getJsonBody();
            System.out.println(body);
            JsonNode response = unirest.post(urlPath)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .asObject(ObjectNode.class)
                    .getBody();

            outputWriterMixin.printToFormat(response);

            return null;
        }
    }
}
