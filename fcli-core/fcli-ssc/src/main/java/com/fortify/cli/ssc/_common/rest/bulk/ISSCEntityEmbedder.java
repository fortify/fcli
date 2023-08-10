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
package com.fortify.cli.ssc._common.rest.bulk;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.core.UnirestInstance;

/**
 * Interface for adding one or more requests and corresponding response consumers
 * to a given {@link SSCBulkRequestBuilder} to embed additional entities into a 
 * given JsonNode. Implementations would usually invoke the 
 * {@link SSCBulkRequestBuilder#request(kong.unirest.HttpRequest, java.util.function.Consumer)}
 * method (or similar method taking a {@link Consumer}) with a {@link Consumer}
 * that takes the response of the request as input and updates the given record
 * to embed response data.
 * 
 * @author rsenden
 */
@FunctionalInterface
public interface ISSCEntityEmbedder {
    void addEmbedRequests(SSCBulkRequestBuilder builder, UnirestInstance unirest, JsonNode record);
}