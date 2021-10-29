package com.fortify.cli.dast.command.entity.scdast.scanstatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.IDefaultOutputColumnsSupplier;
import com.fortify.cli.common.picocli.component.output.IOutputPreTransformer;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.entity.scdast.scanstatus.options.SCDastGetScanStatusOptions;
import com.fortify.cli.dast.command.entity.types.ScanStatusTypes;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

public class SCDastScanStatusCommands {
    private static final String NAME = "scan-status";
    private static final String DESC = "DAST scan status";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IOutputPreTransformer, IDefaultOutputColumnsSupplier {
        @CommandLine.ArgGroup(exclusive = false, heading = "Get a specific scan:%n", order = 1)
        @Getter private SCDastGetScanStatusOptions scanStatusOptions;

        @CommandLine.Mixin
        @Getter private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/scans/"+ scanStatusOptions.getScanId() + "/scan-summary";

            JsonNode response = unirest.get(urlPath)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .asObject(ObjectNode.class)
                    .getBody()
                    .get("item");

            outputOptionsHandler.write(response);

            return null;
        }
        
        @Override
        public JsonNode transform(OutputFormat outputFormat, JsonNode data) {
        	if ( !(data instanceof ObjectNode) ) {
        		throw new IllegalArgumentException("Data is not of expected type");
        	}
        	ObjectNode objectNode = (ObjectNode)data;
        	int scanStatusInt = objectNode.get("scanStatusType").asInt();
            return objectNode.put("scanStatusTypeString", ScanStatusTypes.getStatusString(scanStatusInt));
        }
        
        @Override
        public String getDefaultOutputColumns(OutputFormat outputFormat) {
        	return "scanStatusType#scanStatusTypeString";
        }
    }
}

