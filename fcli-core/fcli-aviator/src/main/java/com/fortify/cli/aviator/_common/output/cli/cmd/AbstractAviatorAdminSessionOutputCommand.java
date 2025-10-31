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
package com.fortify.cli.aviator._common.output.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.config.admin.cli.mixin.AviatorAdminConfigDescriptorSupplier;
import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractAviatorAdminSessionOutputCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private AviatorAdminConfigDescriptorSupplier configDescriptorSupplier;

    @Override
    public final JsonNode getJsonNode() {
        AviatorAdminConfigDescriptor configDescriptor = configDescriptorSupplier.getSessionDescriptor();
        return getJsonNode(configDescriptor);
    }

    protected abstract JsonNode getJsonNode(AviatorAdminConfigDescriptor configDescriptor);

    public abstract boolean isSingular();
}