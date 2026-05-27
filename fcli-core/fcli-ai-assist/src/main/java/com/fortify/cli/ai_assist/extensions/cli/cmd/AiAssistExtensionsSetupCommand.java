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
import com.fortify.cli.ai_assist.extensions.helper.AiAssistExtensionsSourceHandler.DigestMismatchAction;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Setup.CMD_NAME)
public class AiAssistExtensionsSetupCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier, IActionCommandResultSupplier {
    @Mixin @Getter private OutputHelperMixins.Setup outputHelper;

    @ArgGroup(exclusive = true, multiplicity = "1")
    private TargetSelectionGroup targetSelection;

    @Option(names = {"-v", "--version"}, paramLabel = "<version>",
        descriptionKey = "fcli.ai-assist.extensions.version",
        defaultValue = "latest")
    private String version;

    @Option(names = {"-s", "--source"}, paramLabel = "<zip|dir>",
        descriptionKey = "fcli.ai-assist.extensions.source")
    private String source;

    @Option(names = {"--content-types"}, split = ",", paramLabel = "<type>",
        descriptionKey = "fcli.ai-assist.extensions.content-types")
    private Set<String> contentTypeFilter;

    @Option(names = {"--on-digest-mismatch"}, paramLabel = "<action>",
        descriptionKey = "fcli.ai-assist.extensions.on-digest-mismatch",
        defaultValue = "fail")
    private DigestMismatchAction onDigestMismatch;

    @Option(names = {"--dry-run"},
        descriptionKey = "fcli.ai-assist.extensions.dry-run")
    private boolean dryRun;

    @Override
    public JsonNode getJsonNode() {
        var customDir = targetSelection.customDir;
        if (customDir != null && (contentTypeFilter == null || contentTypeFilter.isEmpty())) {
            throw new FcliSimpleException("--content-types is required when using --dir");
        }
        return JsonHelper.getObjectMapper().valueToTree(
            AiAssistExtensionsHelper.setup(
                source, version, targetSelection.assistants, targetSelection.autoDetect,
                contentTypeFilter, customDir, onDigestMismatch, dryRun));
    }

    @Override
    public boolean isSingular() { return false; }

    @Override
    public String getActionCommandResult() { return "SETUP"; }

    static class TargetSelectionGroup {
        @Option(names = {"--assistants"}, split = ",", paramLabel = "<name>",
            descriptionKey = "fcli.ai-assist.extensions.assistants")
        Set<String> assistants;

        @Option(names = {"--auto-detect"},
            descriptionKey = "fcli.ai-assist.extensions.auto-detect")
        boolean autoDetect;

        @Option(names = {"--dir"}, paramLabel = "<path>",
            descriptionKey = "fcli.ai-assist.extensions.dir")
        String customDir;
    }
}
