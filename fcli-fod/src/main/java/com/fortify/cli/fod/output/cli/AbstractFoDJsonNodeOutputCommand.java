package com.fortify.cli.fod.output.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;

import kong.unirest.UnirestInstance;

public abstract class AbstractFoDJsonNodeOutputCommand extends AbstractFoDOutputCommand  implements IJsonNodeSupplier {
    @Override
    public final JsonNode getJsonNode() {
        return getJsonNode(getUnirestInstance());
    }
    
    public abstract JsonNode getJsonNode(UnirestInstance unirest);

}
