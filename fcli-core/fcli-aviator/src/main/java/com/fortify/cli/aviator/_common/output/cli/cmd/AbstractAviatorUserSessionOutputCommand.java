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
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractAviatorUserSessionOutputCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private AviatorUserSessionDescriptorSupplier sessionDescriptorSupplier;

    @Override
    public final JsonNode getJsonNode() {
        AviatorUserSessionDescriptor sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        return getJsonNode(sessionDescriptor);
    }

    protected abstract JsonNode getJsonNode(AviatorUserSessionDescriptor sessionDescriptor);

    public abstract boolean isSingular();
}