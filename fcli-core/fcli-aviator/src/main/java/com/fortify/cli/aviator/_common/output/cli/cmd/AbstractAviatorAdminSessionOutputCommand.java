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