/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.tool.definitions.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
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
    @Getter @Option(names={"-s", "--source"}, required = false, descriptionKey="fcli.tool.definitions.update.definitions-source") 
    private String source = ToolDefinitionsHelper.DEFAULT_TOOL_DEFINITIONS_URL;
    @Reflectable
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