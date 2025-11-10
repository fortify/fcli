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
package com.fortify.cli.common.exception;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Utility for formatting exception details. Provides consistent exception formatting
 * across fcli for both terminal output and programmatic access (e.g., MCP responses).
 * 
 * @author Ruud Senden
 */
public final class FcliExceptionHelper {
    
    private FcliExceptionHelper() {
        // Utility class
    }
    
    /**
     * Format exception with full stack trace, respecting fcli exception formatting rules.
     * AbstractFcliException subclasses control their own formatting via getStackTraceString().
     */
    public static String formatException(Exception e) {
        if ( e==null ) { return null; }
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
}
