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
package com.fortify.cli.aviator.diagnose;

import java.security.cert.CertificateException;
import java.util.Locale;

import javax.net.ssl.SSLException;

/**
 * Detects TLS/trust failures from throwables and status descriptions (cause-chain aware).
 */
public final class AviatorTlsFailureDetector {
    private AviatorTlsFailureDetector() {}

    public static boolean isTlsFailure(Throwable throwable, String statusDescription) {
        if (isTlsFailureMessage(statusDescription)) {
            return true;
        }
        return isTlsFailure(throwable);
    }

    public static boolean isTlsFailure(Throwable throwable) {
        for (var current = throwable; current != null; current = current.getCause()) {
            if (current instanceof SSLException || current instanceof CertificateException) {
                return true;
            }
            var typeAndMessage = current.getClass().getName()
                    + " "
                    + (current.getMessage() == null ? "" : current.getMessage());
            if (isTlsFailureMessage(typeAndMessage)) {
                return true;
            }
        }
        return false;
    }

    static boolean isTlsFailureMessage(String description) {
        if (description == null || description.isBlank()) {
            return false;
        }
        var text = description.toLowerCase(Locale.ROOT);
        return text.contains("sslhandshakeexception")
                || text.contains("sslexception")
                || text.contains("sslengine")
                || text.contains("openssl")
                || text.contains("pkix path")
                || text.contains("unable to find valid certification path")
                || text.contains("certificateexception")
                || text.contains("certpathbuilderexception")
                || text.contains("certpathvalidatorexception")
                || text.contains("certificate_unknown")
                || text.contains("unknown_ca")
                || text.contains("certificate_required")
                || text.contains("handshake_failure");
    }
}
