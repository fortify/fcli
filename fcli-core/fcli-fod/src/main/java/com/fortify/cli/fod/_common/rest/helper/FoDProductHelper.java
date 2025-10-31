/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod._common.rest.helper;

import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;

import lombok.SneakyThrows;

// IMPORTANT: When updating/adding any methods in this class, FoDRestCallCommand
// also likely needs to be updated
public class FoDProductHelper implements IProductHelper, IInputTransformer, INextPageUrlProducerSupplier 
{
    public static final FoDProductHelper INSTANCE = new FoDProductHelper(); 
    private FoDProductHelper() {}
    @Override
    public INextPageUrlProducer getNextPageUrlProducer() {
        return FoDPagingHelper.nextPageUrlProducer();
    }
    
    @Override
    public JsonNode transformInput(JsonNode input) {
        return FoDInputTransformer.getItems(input);
    }
    
    public String getApiUrl(String url) {
        URL result = parseUrl(url);
        String host = result.getHost();
        if (!host.startsWith("api.")) {
            result = buildUrlWithHost(result, "api." + host);
        }
        return stripTrailingSlashes(result.toString());
    }

    public String getBrowserUrl(String url) {
        URL result = parseUrl(url);
        String host = result.getHost();
        if (host.startsWith("api.")) {
            result = buildUrlWithHost(result, host.substring(4));
        }
        return stripTrailingSlashes(result.toString());
    }

    private URL parseUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new FcliSimpleException("Malformed URL - %s", e.getMessage());
        }
    }
    
    @SneakyThrows
    private URL buildUrlWithHost(URL original, String host) {
        return new URL(original.getProtocol(), host, original.getPort(), original.getFile());
    }

    private String stripTrailingSlashes(String value) {
        return value.replaceFirst("/+$", "");
    }
    
}