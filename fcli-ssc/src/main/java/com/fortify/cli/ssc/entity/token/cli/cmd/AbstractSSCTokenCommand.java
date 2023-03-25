package com.fortify.cli.ssc.entity.token.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.ssc.entity.token.cli.mixin.SSCTokenCommandUrlConfigMixin;
import com.fortify.cli.ssc.rest.helper.SSCInputTransformer;

import picocli.CommandLine.Mixin;

public abstract class AbstractSSCTokenCommand extends AbstractOutputCommand implements IJsonNodeSupplier, IInputTransformer {
    @Mixin private SSCTokenCommandUrlConfigMixin urlConfigMixin;
    @Mixin private UserCredentialOptions userCredentialOptions;
    
    @Override
    public final JsonNode getJsonNode() {
        return getJsonNode(urlConfigMixin.getUrlConfig(), userCredentialOptions);
    }
    
    @Override
    public JsonNode transformInput(JsonNode input) {
        return SSCInputTransformer.getDataOrSelf(input);
    }

    protected abstract JsonNode getJsonNode(IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig);    
}
