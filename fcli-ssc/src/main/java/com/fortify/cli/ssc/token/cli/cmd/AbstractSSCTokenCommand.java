package com.fortify.cli.ssc.token.cli.cmd;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.basic.AbstractBasicOutputCommand;
import com.fortify.cli.common.output.spi.transform.IInputTransformerSupplier;
import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentialsConfig;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.ssc.rest.helper.SSCInputTransformer;
import com.fortify.cli.ssc.token.cli.mixin.SSCTokenCommandUrlConfigMixin;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import picocli.CommandLine.Mixin;

@FixInjection
public abstract class AbstractSSCTokenCommand extends AbstractBasicOutputCommand implements IInputTransformerSupplier {
    @Inject @ReflectiveAccess private SSCTokenHelper tokenHelper;
    @Mixin private SSCTokenCommandUrlConfigMixin urlConfigMixin;
    @Mixin private UserCredentialOptions userCredentialOptions;
    
    @Override
    protected final JsonNode getJsonNode() {
        return getJsonNode(tokenHelper, urlConfigMixin.getUrlConfig(), userCredentialOptions);
    }
    
    @Override
    public UnaryOperator<JsonNode> getInputTransformer() {
        return SSCInputTransformer::getDataOrSelf;
    }

    protected abstract JsonNode getJsonNode(SSCTokenHelper tokenHelper, IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig);    
}
