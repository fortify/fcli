package com.fortify.cli.scm.github.cli.mixin;

import java.util.function.Function;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.runner.GenericUnirestFactory;
import com.fortify.cli.common.rest.runner.IUnirestRunner;
import com.fortify.cli.common.rest.runner.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.runner.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.FixInjection;

import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Setter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@FixInjection
public class GitHubUnirestRunnerMixin implements IUnirestRunner {
    @Setter(onMethod=@__({@Inject})) private GenericUnirestFactory genericUnirestFactory;
    @Mixin private GitHubUrlConfigOptions urlConfigOptions;
    @Option(names={"-t", "--github-token"}, required = true, descriptionKey = "fcli.scm.github.token")
    private String token;
    
    @Override
    public <R> R run(Function<UnirestInstance, R> f) {
        if ( f == null ) { throw new IllegalStateException("Function may not be null"); }
        try ( var unirest = genericUnirestFactory.createUnirestInstance() ) {
            configure(unirest);
            return f.apply(unirest);
        }
    }
    
    private void configure(UnirestInstance unirest) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfigOptions);
        ProxyHelper.configureProxy(unirest, "github", urlConfigOptions.getUrl());
        unirest.config().addDefaultHeader("Authorization", "Bearer "+token);
    }
}
