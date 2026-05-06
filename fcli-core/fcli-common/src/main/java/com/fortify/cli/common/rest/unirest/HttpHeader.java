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
 * Standard HTTP header name constants for use with Unirest requests.
 * Use these with {@code headerReplace()} rather than convenience methods like
 * {@code accept()} or {@code contentType()}, which add headers instead of
 * replacing existing ones — causing duplicate headers when defaults are configured.
 */
public final class HttpHeader {
    public static final String ACCEPT          = "Accept";
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String AUTHORIZATION   = "Authorization";
    public static final String CONTENT_TYPE    = "Content-Type";

    private HttpHeader() {}
}
