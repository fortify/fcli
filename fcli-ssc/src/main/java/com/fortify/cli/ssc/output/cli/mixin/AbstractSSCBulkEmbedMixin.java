package com.fortify.cli.ssc.output.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.ssc.rest.bulk.ISSCEntityEmbedderSupplier;
import com.fortify.cli.ssc.rest.bulk.SSCBulkEmbedder;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

public abstract class AbstractSSCBulkEmbedMixin implements IInputTransformer {
    @Mixin private CommandHelperMixin commandHelper;
    private SSCBulkEmbedder bulkEmbedder;
    
    @Override
    public final JsonNode transformInput(JsonNode input) {
        if ( bulkEmbedder==null ) { bulkEmbedder = new SSCBulkEmbedder(getEmbedSuppliers()); }
        UnirestInstance unirest = commandHelper
                .getCommandAs(IUnirestInstanceSupplier.class)
                .orElseThrow().getUnirestInstance();
        bulkEmbedder.transformInput(unirest, input);
        return input;
    }
    
    protected abstract ISSCEntityEmbedderSupplier[] getEmbedSuppliers();
}
