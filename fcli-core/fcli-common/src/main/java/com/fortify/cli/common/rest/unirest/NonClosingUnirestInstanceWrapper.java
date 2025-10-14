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
package com.fortify.cli.common.rest.unirest;

import kong.unirest.Config;
import kong.unirest.GetRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.JsonPatchRequest;
import kong.unirest.UnirestInstance;

public class NonClosingUnirestInstanceWrapper extends UnirestInstance {
    private final UnirestInstance wrappee;
    
    public NonClosingUnirestInstanceWrapper(UnirestInstance wrappee) {
    super(wrappee.config());
    this.wrappee = wrappee;
    }

    public Config config() {
        return wrappee.config();
    }

    public void shutDown() {
        // Do nothing
        // wrappee.shutDown();
    }

    public void shutDown(boolean clearOptions) {
        // Do nothing
        //wrappee.shutDown(clearOptions);
    }

    public GetRequest get(String url) {
        return wrappee.get(url);
    }

    public GetRequest head(String url) {
        return wrappee.head(url);
    }

    public GetRequest options(String url) {
        return wrappee.options(url);
    }

    public HttpRequestWithBody post(String url) {
        return wrappee.post(url);
    }

    public HttpRequestWithBody delete(String url) {
        return wrappee.delete(url);
    }

    public HttpRequestWithBody patch(String url) {
        return wrappee.patch(url);
    }

    public JsonPatchRequest jsonPatch(String url) {
        return wrappee.jsonPatch(url);
    }

    public void close() {
        // Do nothing
        //wrappee.close();
    }

    public boolean equals(Object obj) {
        return wrappee.equals(obj);
    }

    public int hashCode() {
        return wrappee.hashCode();
    }

    public HttpRequestWithBody put(String url) {
        return wrappee.put(url);
    }

    public HttpRequestWithBody request(String method, String url) {
        return wrappee.request(method, url);
    }

    public boolean isRunning() {
        return wrappee.isRunning();
    }

    public String toString() {
        return wrappee.toString();
    }
    
    
}
