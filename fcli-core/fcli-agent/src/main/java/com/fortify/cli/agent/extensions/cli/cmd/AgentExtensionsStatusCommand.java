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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.agent.extensions.helper.AgentExtensionsInstaller;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Status.CMD_NAME)
public class AgentExtensionsStatusCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier {
    @Mixin @Getter private OutputHelperMixins.Status outputHelper;

    @Override
    public JsonNode getJsonNode() {
        return JsonHelper.getObjectMapper().valueToTree(
            AgentExtensionsInstaller.status());
    }

    @Override
    public boolean isSingular() { return false; }
}
