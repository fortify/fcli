package com.fortify.cli.aviator._common.output.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;

public abstract class AbstractAviatorJsonNodeOutputCommand extends AbstractOutputCommand implements IJsonNodeSupplier {

    @Override
    public final JsonNode getJsonNode() {
        return getJsonNodeInternal();
    }

    protected abstract JsonNode getJsonNodeInternal();

    public abstract boolean isSingular();
}