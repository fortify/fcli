package com.fortify.cli.sc_sast.output.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.sc_sast.session.cli.mixin.AbstractSCSastUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc.rest.helper.SSCInputTransformer;
import com.fortify.cli.ssc.rest.helper.SSCPagingHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public class SCSastSSCProductHelperMixin extends AbstractSCSastUnirestInstanceSupplierMixin
    implements IProductHelper, IInputTransformer, IUnirestInstanceSupplier, INextPageUrlProducerSupplier
{
    @Override
    public JsonNode transformInput(JsonNode input) {
        return SSCInputTransformer.getDataOrSelf(input);
    }
    
    @Override
    public INextPageUrlProducer getNextPageUrlProducer(HttpRequest<?> originalRequest) {
        return SSCPagingHelper.nextPageUrlProducer();
    }
    
    @Override
    public UnirestInstance getUnirestInstance() {
        return getSscUnirestInstance();
    }
}