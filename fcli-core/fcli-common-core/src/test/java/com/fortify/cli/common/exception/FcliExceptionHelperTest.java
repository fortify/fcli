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
package com.fortify.cli.common.exception;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

public class FcliExceptionHelperTest {
    @Test
    void formatExceptionShowsPkixGuidanceWithoutStackTrace() {
        var sslException = new SSLHandshakeException(
                "PKIX path building failed: unable to find valid certification path to requested target");
        sslException.initCause(new CertificateException("certificate chain is not trusted"));

        String formatted = FcliExceptionHelper.formatException(new RuntimeException("wrapper", sslException));

        assertTrue(formatted.contains("FcliSimpleException: PKIX SSL certificate validation failed."));
        assertTrue(formatted.contains("Technical summary:"));
        assertTrue(formatted.contains("\nat ") || formatted.contains("\n\tat "));
        assertTrue(formatted.contains("fcli config truststore add-trusted-url <url>"));
        assertTrue(formatted.contains("Troubleshooting steps:"));
    }

    @Test
    void formatExceptionShowsPkixGuidanceForWrappedUnirestStyleException() {
        var sslException = new SSLHandshakeException(
                "(certificate_unknown) PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: "
                        + "unable to find valid certification path to requested target");
        sslException.initCause(new CertificateException(
                "sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target"));
        var unirestStyleException = new RuntimeException("kong.unirest.UnirestException: " + sslException.getMessage(), sslException);

        String formatted = FcliExceptionHelper.formatException(unirestStyleException);

        assertTrue(formatted.contains("FcliSimpleException: PKIX SSL certificate validation failed."));
        assertTrue(formatted.contains("fcli config truststore add-trusted-url <url>"));
        assertTrue(formatted.contains("Technical summary:"));
        assertTrue(formatted.contains("\nat ") || formatted.contains("\n\tat "));
        assertFalse(formatted.contains("kong.unirest.DefaultInterceptor.onFail"));
    }

    @Test
    void formatExceptionKeepsStackTraceForNonPkix() {
        Exception e = new Exception("generic failure");

        String formatted = FcliExceptionHelper.formatException(e);

        assertTrue(formatted.contains("java.lang.Exception: generic failure"));
        assertTrue(formatted.contains("\n\tat "));
    }
}
