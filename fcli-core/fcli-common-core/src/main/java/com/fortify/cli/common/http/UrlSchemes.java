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
package com.fortify.cli.common.http;

/**
 * Shared URL scheme detection (RFC 3986 scheme production, lenient for CLI inputs).
 * Policy after detection (prepend https, reject non-https, …) stays with the caller.
 */
public final class UrlSchemes {
    private static final String SCHEME_PREFIX = "^[a-zA-Z][a-zA-Z0-9+\\-.]*://.*$";

    private UrlSchemes() {}

    /** True when {@code url} starts with a scheme and {@code ://}. */
    public static boolean hasScheme(String url) {
        return url != null && url.matches(SCHEME_PREFIX);
    }
}
