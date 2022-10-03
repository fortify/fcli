package com.fortify.cli.ssc.token.cli.cmd;

import com.fortify.cli.common.rest.runner.config.IUrlConfig;
import com.fortify.cli.common.rest.runner.config.IUserCredentialsConfig;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.ssc.token.cli.mixin.SSCTokenCommandUrlConfigMixin;
import com.fortify.cli.ssc.token.helper.SSCTokenHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import picocli.CommandLine.Mixin;

@ReflectiveAccess @FixInjection
public abstract class AbstractSSCTokenCommand implements Runnable {
    @Inject private SSCTokenHelper tokenHelper;
    @Mixin private SSCTokenCommandUrlConfigMixin urlConfigMixin;
    @Mixin private UserCredentialOptions userCredentialOptions;
    
    @Override
    public void run() {
        run(tokenHelper, urlConfigMixin.getUrlConfig(), userCredentialOptions);
    }

    protected abstract void run(SSCTokenHelper tokenHelper, IUrlConfig urlConfig, IUserCredentialsConfig userCredentialsConfig);
}
