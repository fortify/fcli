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
package com.fortify.cli.agent.extensions.cli.cmd;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.agent.extensions.cli.mixin.AgentExtensionsAssistantFilterMixin;
import com.fortify.cli.agent.extensions.cli.mixin.AgentExtensionsSourceMixin;
import com.fortify.cli.agent.extensions.helper.AgentExtensionsInstaller;
import com.fortify.cli.agent.extensions.helper.AgentExtensionsInstaller.PolicyAction;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class AgentExtensionsUpdateCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Mixin @Getter private OutputHelperMixins.Update outputHelper;
    @Mixin private AgentExtensionsAssistantFilterMixin assistantFilter;
    @Mixin private AgentExtensionsSourceMixin sourceMixin;

    @Option(names = {"--dir"}, paramLabel = "<path>",
        descriptionKey = "fcli.agent.extensions.dir")
    private String customDir;

    @Option(names = {"--content-types"}, split = ",", paramLabel = "<type>",
        descriptionKey = "fcli.agent.extensions.content-types")
    private Set<String> contentTypeFilter;

    @Option(names = {"--on-invalid-signature"}, paramLabel = "<action>",
        descriptionKey = "fcli.agent.extensions.on-invalid-signature",
        defaultValue = "fail")
    private PolicyAction onInvalidSignature;

    @Option(names = {"--on-unsigned"}, paramLabel = "<action>",
        descriptionKey = "fcli.agent.extensions.on-unsigned",
        defaultValue = "fail")
    private PolicyAction onUnsigned;

    @Option(names = {"--on-invalid-version"}, paramLabel = "<action>",
        descriptionKey = "fcli.agent.extensions.on-invalid-version",
        defaultValue = "fail")
    private PolicyAction onInvalidVersion;

    @Option(names = {"-y", "--confirm"},
        descriptionKey = "fcli.agent.extensions.confirm")
    private boolean confirm;

    @Option(names = {"--dry-run"},
        descriptionKey = "fcli.agent.extensions.dry-run")
    private boolean dryRun;

    @Override
    public JsonNode getJsonNode() {
        return JsonHelper.getObjectMapper().valueToTree(
            AgentExtensionsInstaller.update(
                sourceMixin.getSource(),
                assistantFilter.getAssistants(),
                assistantFilter.getExcludeAssistants(),
                contentTypeFilter,
                customDir,
                onInvalidSignature,
                onUnsigned,
                onInvalidVersion,
                dryRun));
    }

    @Override
    public boolean isSingular() { return false; }

    @Override
    public String getActionCommandResult() { return "UPDATED"; }
}
