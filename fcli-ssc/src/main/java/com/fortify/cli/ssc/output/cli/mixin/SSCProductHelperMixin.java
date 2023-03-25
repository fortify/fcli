package com.fortify.cli.ssc.output.cli.mixin;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenHelper;
import com.fortify.cli.ssc.rest.helper.SSCInputTransformer;
import com.fortify.cli.ssc.rest.helper.SSCPagingHelper;
import com.fortify.cli.ssc.session.helper.SSCSessionDescriptor;
import com.fortify.cli.ssc.session.helper.SSCSessionHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class SSCProductHelperMixin extends AbstractSessionUnirestInstanceSupplierMixin<SSCSessionDescriptor>
    implements IProductHelper, IInputTransformer, INextPageUrlProducerSupplier, 
               IHttpRequestUpdater
{
    @Getter private UnaryOperator<JsonNode> inputTransformer = SSCInputTransformer::getDataOrSelf;
    
    @Override
    public INextPageUrlProducer getNextPageUrlProducer(HttpRequest<?> originalRequest) {
        return SSCPagingHelper.nextPageUrlProducer();
    }
    
    @Override
    public JsonNode transformInput(JsonNode input) {
        return SSCInputTransformer.getDataOrSelf(input);
    }
    
    @Override
    public final HttpRequest<?> updateRequest(HttpRequest<?> request) {
        /* TODO Re-implement this
        ISSCQParamGeneratorSupplier qParamGeneratorSupplier = outputHelper.getCommandAs(ISSCQParamGeneratorSupplier.class);
        if ( qParamGeneratorSupplier!=null ) {
            request = qParamGeneratorSupplier.getQParamGenerator().addQParam(outputHelper, request);
        }
        */
        return request;
    }
    
    @Override
    public final SSCSessionDescriptor getSessionDescriptor(String sessionName) {
        return SSCSessionHelper.instance().get(sessionName, true);
    }
    
    @Override
    public final void configure(UnirestInstance unirest, SSCSessionDescriptor sessionDescriptor) {
        SSCTokenHelper.configureUnirest(unirest, sessionDescriptor.getUrlConfig(), sessionDescriptor.getActiveToken());
    }
}
