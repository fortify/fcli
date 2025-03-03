package com.fortify.cli.aviator._common.output.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
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