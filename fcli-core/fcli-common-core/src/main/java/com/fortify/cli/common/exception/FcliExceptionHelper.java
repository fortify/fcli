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

import java.security.cert.CertPathValidatorException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Utility for formatting exception details. Provides consistent exception formatting
 * across fcli for both terminal output and programmatic access (e.g., MCP responses).
 * 
 * @author Ruud Senden
 */
public final class FcliExceptionHelper {
    private static final String PKIX_PATH_BUILDING_FAILED = "PKIX path building failed";
    private static final String PKIX_TRUST_PATH_NOT_FOUND = "unable to find valid certification path";
    private static final String PKIX_SUN_CERT_PATH_BUILDER_EXCEPTION = "SunCertPathBuilderException";
    
    private FcliExceptionHelper() {
        // Utility class
    }
    
    /**
     * Format exception with full stack trace, respecting fcli exception formatting rules.
     * AbstractFcliException subclasses control their own formatting via getStackTraceString().
     */
    public static String formatException(Exception e) {
        if ( e==null ) { return null; }
        if ( isPkixRelatedException(e) ) {
            return formatPkixRelatedException(e);
        }
        return (e instanceof AbstractFcliException) 
            ? formatFcliException((AbstractFcliException)e) 
            : formatNonFcliException(e);
    }
    
    /**
     * Extract concise error message from exception.
     * Falls back to class name if message is null.
     */
    public static String getErrorMessage(Exception e) {
        if ( e==null ) { return "Unknown error"; }
        var message = e.getMessage();
        return message!=null ? message : e.getClass().getSimpleName();
    }
    
    private static String formatFcliException(AbstractFcliException e) {
        return e.getStackTraceString();
    }

    private static String formatNonFcliException(Exception e) {
        return ExceptionUtils.getStackTrace(e);
    }

    private static boolean isPkixRelatedException(Throwable t) {
        while ( t!=null ) {
            if ( t instanceof SSLHandshakeException || t instanceof CertPathValidatorException ) {
                return true;
            }
            String className = t.getClass().getName();
            if ( className.contains(PKIX_SUN_CERT_PATH_BUILDER_EXCEPTION) ) {
                return true;
            }
            String message = t.getMessage();
            if ( message!=null && (message.contains(PKIX_PATH_BUILDING_FAILED) || message.contains(PKIX_TRUST_PATH_NOT_FOUND)) ) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static String formatPkixRelatedException(Exception e) {
        RootCauseSummary technicalSummary = summarizeRootCause(e);
        List<String> lines = new ArrayList<>();
        lines.add("FcliSimpleException: PKIX SSL certificate validation failed.");
        lines.add("The server certificate chain is not trusted by fcli.");
        lines.add("");
        lines.add("Troubleshooting steps:");
        lines.add("1. Verify the server sends a complete certificate chain.");
        lines.add("2. Verify the chain ends in a trusted root/intermediate CA.");
        lines.add("3. Configure fcli trust store via command or env vars.");
        lines.add("   Command: fcli config truststore set <file>");
        lines.add("   Env vars: FCLI_TRUSTSTORE, FCLI_TRUSTSTORE_TYPE,");
        lines.add("             FCLI_TRUSTSTORE_PWD");
        lines.add("4. To trust only this endpoint in fcli, run:");
        lines.add("   fcli config truststore add-trusted-url <url>");
        lines.add("5. Use --log-level TRACE to inspect loaded trusted CAs.");
        lines.add("");
        lines.add("Technical summary: "+technicalSummary.summary());
        if ( technicalSummary.firstFrame()!=null ) {
            lines.add("\tat "+technicalSummary.firstFrame());
        }
        lines.add("Enable DEBUG logging for full stack trace in fcli log file.");
        return String.join("\n", lines);
    }

    private static RootCauseSummary summarizeRootCause(Throwable e) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        Throwable relevant = rootCause==null ? e : rootCause;
        String message = relevant.getMessage();
        String summary = (message==null || message.isBlank())
                ? relevant.getClass().getSimpleName()
                : relevant.getClass().getSimpleName()+": "+message;
        StackTraceElement[] stackTrace = relevant.getStackTrace();
        return new RootCauseSummary(summary, stackTrace!=null && stackTrace.length>0 ? stackTrace[0] : null);
    }

    private record RootCauseSummary(String summary, StackTraceElement firstFrame) {}

}
