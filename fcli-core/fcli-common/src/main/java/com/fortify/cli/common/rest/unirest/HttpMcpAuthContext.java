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
package com.fortify.cli.common.rest.unirest;

/**
 * ThreadLocal holder for HTTP MCP authentication context.
 * Set by the Jetty auth filter on the servlet thread, read by
 * SSC session resolution to build a synthetic session descriptor.
 */
public final class HttpMcpAuthContext {
    private HttpMcpAuthContext() {}

    public record AuthInfo(String sscUrl, char[] token) {}

    private static final ThreadLocal<AuthInfo> CURRENT = new ThreadLocal<>();

    public static AuthInfo get() {
        return CURRENT.get();
    }

    public static void set(AuthInfo info) {
        CURRENT.set(info);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
