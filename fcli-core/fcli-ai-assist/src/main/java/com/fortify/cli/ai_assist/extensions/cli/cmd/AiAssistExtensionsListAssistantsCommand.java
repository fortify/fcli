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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.ai_assist.extensions.helper.AiAssistExtensionsHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "list-assistants")
public class AiAssistExtensionsListAssistantsCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier {
    @Mixin @Getter private OutputHelperMixins.TableNoQuery outputHelper;

    @Option(names = {"--detect"},
        descriptionKey = "fcli.ai-assist.extensions.detect")
    private boolean detect;

    @Override
    public JsonNode getJsonNode() {
        return JsonHelper.getObjectMapper().valueToTree(
            AiAssistExtensionsHelper.listAssistants(detect));
    }

    @Override
    public boolean isSingular() { return false; }
}
