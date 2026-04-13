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
package com.fortify.cli.ssc._common.rest.helper;

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
 * that will retry GET requests if the server responds with an HTTP 502 (Bad Gateway)
 * or 503 (Service Unavailable) response, using exponential backoff with jitter.
 * Non-GET requests are not retried to avoid the risk of duplicate side effects
 * (e.g., creating duplicate entities).
 */
public final class SSCRetryStrategy implements ServiceUnavailableRetryStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(SSCRetryStrategy.class);
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;
    private static final long MAX_JITTER_MS = 500;
    private final ThreadLocal<Long> interval = new ThreadLocal<Long>();

    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        int statusCode = response.getStatusLine().getStatusCode();
        if ( executionCount <= MAX_RETRIES && (statusCode == 502 || statusCode == 503) ) {
            HttpRequest request = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
            String method = request.getRequestLine().getMethod();
            if ( !"GET".equalsIgnoreCase(method) ) {
                LOG.debug("SSC returned {}; not retrying non-GET request ({} {})", statusCode, method, request.getRequestLine().getUri());
                return false;
            }
            long delay = BASE_DELAY_MS * (1L << (executionCount - 1));
            long jitter = ThreadLocalRandom.current().nextLong(MAX_JITTER_MS + 1);
            long totalDelay = delay + jitter;
            LOG.debug("SSC returned {}; retrying GET request (attempt {}/{}) after {} ms", statusCode, executionCount, MAX_RETRIES, totalDelay);
            interval.set(totalDelay);
            return true;
        }
        return false;
    }

    public long getRetryInterval() {
        Long result = interval.get();
        return result == null ? -1 : result;
    }
}
