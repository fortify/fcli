package com.fortify.cli.tool.definitions.cli.cmd;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name=OutputHelperMixins.Update.CMD_NAME)
public class ToolDefinitionsUpdateCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Mixin @Getter private OutputHelperMixins.Update outputHelper;
    private static final String DEFAULT_URL = "https://github.com/fortify/tool-definitions/releases/download/v1/tool-definitions.yaml.zip";
    @Getter @Option(names={"-s", "--source"}, required = false, descriptionKey="fcli.tool.definitions.update.definitions-source") 
    private String source = DEFAULT_URL;
    static class UpdateMode {
        @Option(names={"-f", "--force"}, required = false, descriptionKey="fcli.tool.definitions.update.force", paramLabel = "Force Update")
        boolean forceUpdates;
        @Option(names={"--max-age"}, required = false, descriptionKey="fcli.tool.definitions.update.max-age", paramLabel = "timeout")
        String maxAge;
    }
    @ArgGroup(exclusive = true, multiplicity = "0..1")
    @Getter private UpdateMode updateMode = new UpdateMode();

    @Override
    public JsonNode getJsonNode() {
        if (updateMode.forceUpdates && updateMode.maxAge != null && !updateMode.maxAge.isEmpty()) {
            throw new IllegalArgumentException("You must specify only one of --force or --max-age, not both.");
        }
        return JsonHelper.getObjectMapper().valueToTree(
            ToolDefinitionsHelper.updateToolDefinitions(
                source,
                updateMode.forceUpdates,
                updateMode.maxAge
            )
        );
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }
}