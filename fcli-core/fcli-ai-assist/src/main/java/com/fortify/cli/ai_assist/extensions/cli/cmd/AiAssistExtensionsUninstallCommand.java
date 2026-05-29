/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.ai_assist.extensions.cli.cmd;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.ai_assist.extensions.helper.AiAssistExtensionsHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Uninstall.CMD_NAME)
public class AiAssistExtensionsUninstallCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Mixin @Getter private OutputHelperMixins.Uninstall outputHelper;

    @Option(names = {"--content-types"}, split = ",", paramLabel = "<type>",
        descriptionKey = "fcli.ai-assist.extensions.content-types")
    private Set<String> contentTypeFilter;

    @Option(names = {"--dir"}, paramLabel = "<path>",
        descriptionKey = "fcli.ai-assist.extensions.uninstall.dir")
    private String customDir;

    @Option(names = {"--dry-run"},
        descriptionKey = "fcli.ai-assist.extensions.dry-run")
    private boolean dryRun;

    @Override
    public JsonNode getJsonNode() {
        return JsonHelper.getObjectMapper().valueToTree(
            AiAssistExtensionsHelper.uninstall(contentTypeFilter, customDir, dryRun));
    }

    @Override
    public boolean isSingular() { return false; }

    @Override
    public String getActionCommandResult() { return "REMOVED"; }
}
