package com.fortify.cli.sc_dast.output.cli.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionUnirestInstanceSupplierMixin;
import com.fortify.cli.sc_dast.rest.helper.SCDastInputTransformer;
import com.fortify.cli.sc_dast.rest.helper.SCDastPagingHelper;
import com.fortify.cli.sc_dast.session.helper.SCDastSessionDescriptor;
import com.fortify.cli.sc_dast.session.helper.SCDastSessionHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;

public class SCDastProductHelperMixin extends AbstractSessionUnirestInstanceSupplierMixin<SCDastSessionDescriptor> 
    implements IProductHelper, IInputTransformer, INextPageUrlProducerSupplier, IUnirestInstanceSupplier
{
    @Getter @Setter private IOutputHelper outputHelper;
    @Getter private UnaryOperator<JsonNode> inputTransformer = SCDastInputTransformer::getItems;
    
    @Override
    public INextPageUrlProducer getNextPageUrlProducer(HttpRequest<?> originalRequest) {
        return SCDastPagingHelper.nextPageUrlProducer(originalRequest);
    }
    
    @Override
    public JsonNode transformInput(JsonNode input) {
        return SCDastInputTransformer.getItems(input);
    }
    
    @Override
    protected final void configure(UnirestInstance unirest, SCDastSessionDescriptor sessionDescriptor) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionDescriptor.getScDastUrlConfig());
        ProxyHelper.configureProxy(unirest, "sc-dast", sessionDescriptor.getScDastUrlConfig().getUrl());
        unirest.config().requestCompression(false); // TODO Check whether SC DAST suffers from the same issue as SSC, with some requests failing if compression is enabled
        unirest.config().addDefaultHeader("Authorization", "FortifyToken "+new String(sessionDescriptor.getActiveToken()));
    }
    
    @Override
    protected final SCDastSessionDescriptor getSessionDescriptor(String sessionName) {
        return SCDastSessionHelper.instance().get(sessionName, true);
    }
}