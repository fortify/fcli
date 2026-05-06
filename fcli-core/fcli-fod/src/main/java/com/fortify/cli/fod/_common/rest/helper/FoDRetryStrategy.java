/*
 * Copyright 2021-2026 Open Text.
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

import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements an Apache HttpClient 4.x {@link ServiceUnavailableRetryStrategy}
 * that will retry a request if the server responds with an HTTP 429 (TOO_MANY_REQUESTS)
 * response, and will retry GET requests on HTTP 502 (Bad Gateway) or 503
 * (Service Unavailable) with exponential backoff and jitter.
 */
public final class FoDRetryStrategy implements ServiceUnavailableRetryStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(FoDRetryStrategy.class);
    private static final String HEADER_NAME = "X-Rate-Limit-Reset";
    private static final long BASE_DELAY_MS = 1000;
    private static final long MAX_JITTER_MS = 500;
    private int maxRetries = 2;
    private final ThreadLocal<Long> interval = new ThreadLocal<Long>();

    public FoDRetryStrategy maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        if ( executionCount < maxRetries+1 ) {
            int statusCode = response.getStatusLine().getStatusCode();
            if ( statusCode==404 ) {
                // Sometimes it can take a bit of time for FoD to properly register a scan request and
                // possibly other newly created resources, hence we also retry on 404 errors.
                interval.set((long)5000);
                return true;
            } else if ( statusCode==429 ) {
                int retrySeconds = Integer.parseInt(response.getFirstHeader(HEADER_NAME).getValue());
                LOG.debug("Rate-limited request will be retried after "+retrySeconds+" seconds");
                interval.set((long)retrySeconds*1000);
                return true;
            } else if ( statusCode==502 || statusCode==503 ) {
                HttpRequest request = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
                String method = request.getRequestLine().getMethod();
                if ( !"GET".equalsIgnoreCase(method) ) {
                    LOG.debug("FoD returned {}; not retrying non-GET request ({} {})", statusCode, method, request.getRequestLine().getUri());
                    return false;
                }
                long delay = BASE_DELAY_MS * (1L << (executionCount - 1));
                long jitter = ThreadLocalRandom.current().nextLong(MAX_JITTER_MS + 1);
                long totalDelay = delay + jitter;
                LOG.debug("FoD returned {}; retrying GET request (attempt {}/{}) after {} ms", statusCode, executionCount, maxRetries, totalDelay);
                interval.set(totalDelay);
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
