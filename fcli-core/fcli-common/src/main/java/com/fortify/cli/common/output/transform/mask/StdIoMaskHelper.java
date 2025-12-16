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
package com.fortify.cli.common.output.transform.mask;

import java.io.PrintStream;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.regex.MultiPatternReplacer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Singleton helper for installing/uninstalling masking on System.out and System.err.
 * Follows the same design pattern as LogMaskHelper for consistency.
 * Provides fluent API for registering patterns and sensitive values.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StdIoMaskHelper {
    public static final StdIoMaskHelper INSTANCE = new StdIoMaskHelper();
    
    private PrintStream originalOut;
    private PrintStream originalErr;
    private final MultiPatternReplacer multiPatternReplacer = new MultiPatternReplacer();
    private boolean installed = false;
    
    /**
     * Install masking on System.out and System.err.
     * Safe to call multiple times; subsequent calls are ignored.
     */
    public synchronized StdIoMaskHelper install() {
        if (installed) {
            return this;
        }
        
        registerDefaultPatterns();
        
        originalOut = System.out;
        originalErr = System.err;
        
        System.setOut(new MaskingPrintStream(originalOut, this::mask));
        System.setErr(new MaskingPrintStream(originalErr, this::mask));
        
        installed = true;
        return this;
    }
    
    /**
     * Uninstall masking and restore original System.out/err.
     * Safe to call multiple times; subsequent calls are ignored.
     */
    public synchronized StdIoMaskHelper uninstall() {
        if (!installed) {
            return this;
        }
        
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }
        
        originalOut = null;
        originalErr = null;
        installed = false;
        return this;
    }
    
    /**
     * Register a regex pattern for masking sensitive content.
     * Uses capture group 1 for replacement (same as LogMaskHelper).
     * 
     * @param patternString regex pattern with capture group for sensitive part
     * @param replacement replacement string (e.g., "<REDACTED>")
     * @return this for method chaining
     */
    public synchronized StdIoMaskHelper registerPattern(String patternString, String replacement) {
        multiPatternReplacer.registerPattern(patternString, replacement);
        return this;
    }
    
    /**
     * Register a specific sensitive value to be masked.
     * The value will be replaced with the given replacement string.
     * 
     * @param sensitiveValue exact value to mask
     * @param replacement replacement string
     * @return this for method chaining
     */
    public synchronized StdIoMaskHelper registerValue(String sensitiveValue, String replacement) {
        if (StringUtils.isNotBlank(sensitiveValue)) {
            multiPatternReplacer.registerValue(sensitiveValue, replacement);
        }
        return this;
    }
    
    /**
     * Register a sensitive value with default "<REDACTED>" replacement.
     * 
     * @param sensitiveValue exact value to mask
     * @return this for method chaining
     */
    public synchronized StdIoMaskHelper registerValue(String sensitiveValue) {
        return registerValue(sensitiveValue, "<REDACTED>");
    }
    
    /**
     * Apply all registered patterns and values to mask sensitive content.
     * 
     * @param input text that may contain sensitive data
     * @return masked text with sensitive content replaced
     */
    public String mask(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }
        try {
            return multiPatternReplacer.applyReplacements(input, null);
        } catch (Exception e) {
            // Never fail masking - return safe fallback
            return "<MASKED DUE TO ERROR>";
        }
    }
    
    /**
     * Register default patterns for common sensitive data.
     * Called automatically during install().
     */
    private void registerDefaultPatterns() {
        // Authorization headers
        registerPattern("Authorization: (?:[a-zA-Z]+ )?(.*?)(?:\\Q[\\r]\\E|\\Q[\\n]\\E)*\\\"?$", "<REDACTED>");
        
        // Bearer tokens
        registerPattern("(?i)(bearer\\s+)([\\w\\-._~+/]+=*)", "<REDACTED>");
        
        // API keys
        registerPattern("(?i)(api[_-]?key[\"'\\s:=]+)([\\w\\-._~+/]+=*)", "<REDACTED>");
        
        // Passwords
        registerPattern("(?i)(password[\"'\\s:=]+)([^\\s\"']+)", "<REDACTED>");
        
        // Secrets
        registerPattern("(?i)(secret[\"'\\s:=]+)([^\\s\"']+)", "<REDACTED>");
        
        // JSON tokens
        registerPattern("(?:\\\"token\\\"|\\\"access_token\\\"):\\s*\\\"(.*?)\\\"", "<REDACTED TOKEN>");
        
        // X-API-Key headers
        registerPattern("(?i)(x-api-key[\"'\\s:=]+)([^\\s\"']+)", "<REDACTED>");
    }
    
    public synchronized boolean isInstalled() {
        return installed;
    }
}