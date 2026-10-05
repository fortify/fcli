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
package com.fortify.cli.aviator._common.exception;

/**
 * User-facing rejection of a non-{@code https} Aviator target scheme.
 * <p>
 * Typed so diagnose can map endpoint UX without parsing exception message text.
 */
public class UnsupportedAviatorUrlSchemeException extends AviatorSimpleException {
    private static final long serialVersionUID = 1L;

    public static final String STAGE_SUMMARY = "Unsupported URL scheme";
    public static final String STAGE_GUIDANCE = "Use a supported Aviator URL (https://host[:port])";

    private final String scheme;
    private final String providedUrl;

    public UnsupportedAviatorUrlSchemeException(String scheme, String providedUrl) {
        super(STAGE_SUMMARY+" '"+scheme+"'. "+STAGE_GUIDANCE+". Provided URL: "+providedUrl);
        this.scheme = scheme;
        this.providedUrl = providedUrl;
    }

    public String getScheme() {
        return scheme;
    }

    public String getProvidedUrl() {
        return providedUrl;
    }
}
