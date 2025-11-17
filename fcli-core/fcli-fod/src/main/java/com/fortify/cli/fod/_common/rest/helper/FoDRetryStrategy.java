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

import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements an Apache HttpClient 4.x {@link ServiceUnavailableRetryStrategy}
 * that will retry a request if the server responds with an HTTP 429 (TOO_MANY_REQUESTS)
 * response.
 */
public final class FoDRetryStrategy implements ServiceUnavailableRetryStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(FoDRetryStrategy.class);
    private final String HEADER_NAME = "X-Rate-Limit-Reset";
    private int maxRetries = 2;
    private final ThreadLocal<Long> interval = new ThreadLocal<Long>();
    
    public FoDRetryStrategy maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        if ( executionCount < maxRetries+1 ) {
            if ( response.getStatusLine().getStatusCode()==404 ) {
                // Sometimes it can take a bit of time for FoD to properly register a scan request and
                // possibly other newly created resources, hence we also retry on 404 errors.
                interval.set((long)5000);
                return true;
            } else if ( response.getStatusLine().getStatusCode()==429 ) {
                int retrySeconds = Integer.parseInt(response.getFirstHeader(HEADER_NAME).getValue());
                LOG.debug("Rate-limited request will be retried after "+retrySeconds+" seconds");
                interval.set((long)retrySeconds*1000);
                return true;
            }
        }
        return false;
    }

    public long getRetryInterval() {
        Long result = interval.get();
        return result==null ? -1 : result;
    }
}
