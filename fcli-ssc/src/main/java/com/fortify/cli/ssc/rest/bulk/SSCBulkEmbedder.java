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
package com.fortify.cli.ssc.rest.bulk;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.rest.helper.SSCInputTransformer;

import kong.unirest.UnirestInstance;

/**
 * This class takes zero or more {@link ISSCEntityEmbedderSupplier} instances
 * as constructor argument(s), storing the {@link ISSCEntityEmbedder} instances
 * generated by these suppliers, to provide the {@link #transformInput(UnirestInstance, JsonNode)}
 * method that builds and executes a bulk request for inserting embedded data
 * into each JSON node contained in the given input.
 *  
 * @author rsenden
 *
 */
public class SSCBulkEmbedder {
    private final Collection<ISSCEntityEmbedder> embedders;
    
    public SSCBulkEmbedder(ISSCEntityEmbedderSupplier... suppliers) {
        this.embedders = suppliers==null ? null : Stream.of(suppliers)
                .map(ISSCEntityEmbedderSupplier::createEntityEmbedder)
                .collect(Collectors.toList());
    }
    
    public ArrayNode transformInput(UnirestInstance unirest, JsonNode input) {
        var data = SSCInputTransformer.getDataOrSelf(input);
        var records = data instanceof ArrayNode 
                ? (ArrayNode) data
                : JsonHelper.toArrayNode(data);
        if ( embedders!=null ) {
            SSCBulkRequestBuilder builder = new SSCBulkRequestBuilder();
            for ( var record : records ) {
                embedders.forEach(u->u.addEmbedRequests(builder, unirest, record));
            }
            builder.execute(unirest);
        }
        return records;
    }
}