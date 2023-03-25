package com.fortify.cli.scm.github.cli.mixin;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.scm.github.cli.util.GitHubPagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class GitHubProductHelperMixin
    implements IProductHelper, INextPageUrlProducerSupplier
{
    @Mixin private GitHubUrlConfigOptions urlConfigOptions;
    @Option(names={"-t", "--github-token"}, required = true, descriptionKey = "fcli.scm.github.token")
    private String token;
    
    @Override
    public INextPageUrlProducer getNextPageUrlProducer(HttpRequest<?> originalRequest) {
        return GitHubPagingHelper.nextPageUrlProducer();
    }
    
    public UnirestInstance createUnirestInstance() {
        var instance = GenericUnirestFactory.createUnirestInstance();
        configure(instance);
        return instance;
    }
    
    private void configure(UnirestInstance unirest) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfigOptions);
        ProxyHelper.configureProxy(unirest, "github", urlConfigOptions.getUrl());
        unirest.config().addDefaultHeader("Authorization", "Bearer "+token);
    }
}
