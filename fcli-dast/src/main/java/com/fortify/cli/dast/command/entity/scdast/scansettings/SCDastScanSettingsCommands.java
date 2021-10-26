package com.fortify.cli.dast.command.entity.scdast.scansettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.output.OutputWriterMixin;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.SCDastEntityRootCommands;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

public class SCDastScanSettingsCommands {
    private static final String NAME = "scan-settings";
    private static final String DESC = "DAST scan settings";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand {
        @Spec
        CommandLine.Model.CommandSpec spec;

        @Option(names = {"-id", "--scan-settings-id"}, description = "The scan settings id" )
        @Getter private String scanSettingsId;

        @Mixin
        private OutputWriterMixin outputWriterMixin;


        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            if (getScanSettingsId() == null)
            {throw new CommandLine.ParameterException(spec.commandLine(),"Missing Scan Settings Id");}

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

