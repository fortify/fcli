/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.entity.appversion_attribute.helper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.ssc.entity.attribute_definition.helper.SSCAttributeDefinitionHelper;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc.rest.bulk.SSCBulkRequestBuilder.SSCBulkResponse;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

/**
 * This class provides various helper methods for retrieving and formatting
 * application version attributes, combining data from SSC's attributeDefinitions
 * and application version attributes endpoints. In order to properly use this class,
 * command implementions will need to call the {@link #execute(UnirestInstance, String)}
 * method in their runWithUnirest() method, and register the {@link #transformRecord(JsonNode)}
 * method as a record transformer in their output configuration.
 * 
 * @author rsenden
 */
public final class SSCAppVersionAttributeListHelper {
    private ArrayNode cachedAttributeDefinitions;
    private Map<String, HttpRequest<?>> requests = new LinkedHashMap<>();
    private Set<String> attrIdsToInclude = null;
    
    /**
     * The {@link #execute(UnirestInstance, String)} method by default calls
     * SSC's attributeDefinitions and application version attributes endpoints.
     * This method allows for adding additional requests, for example for
     * updating attribute values before querying them.
     *  
     * @param name
     * @param request
     * @return this for chaining
     */
    public SSCAppVersionAttributeListHelper request(String name, HttpRequest<?> request) {
        requests.put(name, request);
        return this;
    }
    
    /**
     * The {@link #execute(UnirestInstance, String)} method by default calls
     * SSC's attributeDefinitions endpoint to query attribute definitions, but
     * if you already have an {@link SSCAttributeDefinitionHelper} instance,
     * this can be used instead to prevent querying for attribute definitions
     * multiple times. 
     * @param attributeDefinitionHelper
     * @return
     */
    public SSCAppVersionAttributeListHelper attributeDefinitionHelper(SSCAttributeDefinitionHelper attributeDefinitionHelper) {
        this.cachedAttributeDefinitions = attributeDefinitionHelper.getAttributeDefinitions();
        return this;
    }
    
    public SSCAppVersionAttributeListHelper attrIdsToInclude(Set<String> attrIdsToInclude) {
        this.attrIdsToInclude = attrIdsToInclude;
        return this;
    }
    
    /**
     * Execute a bulk request, containing requests as added through the {@link #request(String, HttpRequest)}
     * method, and requests for querying attribute definition and attribute data. The attribute definition data
     * and attribute data will then be combined and returned.
     *  
     * @param unirest
     * @param applicationVersionId
     * @return
     */
    public JsonNode execute(UnirestInstance unirest, String applicationVersionId) {
        SSCBulkResponse bulkResponse = buildBulkRequest(unirest, applicationVersionId).execute(unirest);
        if ( cachedAttributeDefinitions==null ) {
            cachedAttributeDefinitions = (ArrayNode)bulkResponse.data("_attrDefs");
        }
        JsonNode attributesBody = bulkResponse.body("_attrs");
        // We use the attribute definitions as the basis, to be able to also 
        // return attributes that have no value (as SSC's application version 
        // attributes endpoint only returns attributes that have a value).
        return new SSCAppVersionAttributeHelper(cachedAttributeDefinitions, attrIdsToInclude)
                .mergeAttributeDefinitions(attributesBody);
    }

    /**
     * Build the SSC bulk request, adding any requests added through the {@link #request(String, HttpRequest)}
     * method, and the necessary requests to query attribute and attribute definition data. 
     * @param unirest
     * @param applicationVersionId
     * @return
     */
    private SSCBulkRequestBuilder buildBulkRequest(UnirestInstance unirest, String applicationVersionId) {
        SSCBulkRequestBuilder bulkRequest = new SSCBulkRequestBuilder();
        requests.entrySet().forEach(e->bulkRequest.request(e.getKey(), e.getValue()));
        if ( cachedAttributeDefinitions==null ) {
            bulkRequest.request("_attrDefs", unirest.get("/api/v1/attributeDefinitions?limit=-1&fields=id,guid,name,category&orderby=category,name"));
        }
        bulkRequest.request("_attrs", unirest.get("/api/v1/projectVersions/{id}/attributes").routeParam("id", applicationVersionId));
        return bulkRequest;
    }
}
