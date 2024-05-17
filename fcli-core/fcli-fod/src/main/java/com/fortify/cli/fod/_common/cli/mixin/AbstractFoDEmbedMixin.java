/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod._common.cli.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.fod._common.rest.embed.FoDEmbedder;
import com.fortify.cli.fod._common.rest.embed.IFoDEntityEmbedderSupplier;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

public abstract class AbstractFoDEmbedMixin implements IRecordTransformer {
    @Mixin private CommandHelperMixin commandHelper;
    private FoDEmbedder embedder;
    
    @Override
    public final JsonNode transformRecord(JsonNode record) {
        if ( embedder==null ) { embedder = new FoDEmbedder(getEmbedSuppliers()); }
        UnirestInstance unirest = commandHelper
                .getCommandAs(IUnirestInstanceSupplier.class)
                .orElseThrow().getUnirestInstance();
        embedder.transformRecord(unirest, record);
        return record;
    }
    
    protected abstract IFoDEntityEmbedderSupplier[] getEmbedSuppliers();
}
