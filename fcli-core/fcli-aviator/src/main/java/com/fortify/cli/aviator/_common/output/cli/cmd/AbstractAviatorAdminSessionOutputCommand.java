package com.fortify.cli.aviator._common.output.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.aviator._common.session.admin.cli.mixin.AviatorAdminSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.session.admin.helper.AviatorAdminSessionDescriptor;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractAviatorAdminSessionOutputCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    @Getter @Mixin private AviatorAdminSessionDescriptorSupplier sessionDescriptorSupplier;

    @Override
    public final JsonNode getJsonNode() {
        AviatorAdminSessionDescriptor sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        return getJsonNode(sessionDescriptor);
    }

    protected abstract JsonNode getJsonNode(AviatorAdminSessionDescriptor sessionDescriptor);

    public abstract boolean isSingular();
}