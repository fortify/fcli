package com.fortify.cli.sc_sast.output.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.sc_sast.rest.helper.SCSastInputTransformer;
import com.fortify.cli.sc_sast.session.cli.mixin.AbstractSCSastUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;

public class SCSastControllerProductHelperMixin extends AbstractSCSastUnirestInstanceSupplierMixin
    implements IProductHelper, IInputTransformer, IUnirestInstanceSupplier
{
    @Override
    public JsonNode transformInput(JsonNode input) {
        return SCSastInputTransformer.getItems(input);
    }
    
    @Override
    public UnirestInstance getUnirestInstance() {
        return getControllerUnirestInstance();
    }
}