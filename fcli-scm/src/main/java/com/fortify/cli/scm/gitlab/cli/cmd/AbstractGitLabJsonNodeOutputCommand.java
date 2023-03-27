package com.fortify.cli.scm.gitlab.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;

import kong.unirest.UnirestInstance;

public abstract class AbstractGitLabJsonNodeOutputCommand extends AbstractGitLabOutputCommand  implements IJsonNodeSupplier {
    @Override
    public final JsonNode getJsonNode() {
        try ( var unirest = getProductHelper().createUnirestInstance() ) {
            return getJsonNode(unirest);
        }
    }
    
    public abstract JsonNode getJsonNode(UnirestInstance unirest);

}
