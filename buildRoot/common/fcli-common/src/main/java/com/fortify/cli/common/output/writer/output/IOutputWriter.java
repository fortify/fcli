/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.common.output.writer.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;

public interface IOutputWriter {

    void write(JsonNode jsonNode);

    void write(HttpRequest<?> httpRequest);

    void write(HttpRequest<?> httpRequest, INextPageUrlProducer nextPageUrlProducer);

    void write(HttpResponse<JsonNode> httpResponse);

}