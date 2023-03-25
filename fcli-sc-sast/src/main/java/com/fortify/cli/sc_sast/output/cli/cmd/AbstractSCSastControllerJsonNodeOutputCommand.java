package com.fortify.cli.sc_sast.output.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;

import kong.unirest.UnirestInstance;

public abstract class AbstractSCSastControllerJsonNodeOutputCommand extends AbstractSCSastControllerOutputCommand  implements IJsonNodeSupplier {
    @Override
    public final JsonNode getJsonNode() {
        return getJsonNode(getUnirestInstance());
    }
    
    public abstract JsonNode getJsonNode(UnirestInstance unirest);

}
