package com.fortify.cli.scm.gitlab.cli.mixin;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.scm.gitlab.cli.util.GitLabPagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class GitLabProductHelperMixin
    implements IProductHelper, INextPageUrlProducerSupplier
{
    @Mixin private GitLabUrlConfigOptions urlConfigOptions;
    @Option(names={"-t", "--gitlab-token"}, required = true, descriptionKey = "fcli.scm.gitlab.token")
    private String token;
    
    @Override
    public INextPageUrlProducer getNextPageUrlProducer(HttpRequest<?> originalRequest) {
        return GitLabPagingHelper.nextPageUrlProducer();
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
        ProxyHelper.configureProxy(unirest, "gitlab", urlConfigOptions.getUrl());
        unirest.config().addDefaultHeader("PRIVATE-TOKEN", token);
    }
}
