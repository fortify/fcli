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

import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogSensitivityLevel;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Singleton helper for installing/uninstalling masking on System.out and System.err.
 * Delegates to LogMaskHelper for pattern/value registration to avoid duplication.
 * Respects the configured --log-mask level for sensitivity-based masking.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StdIoMaskHelper {
    public static final StdIoMaskHelper INSTANCE = new StdIoMaskHelper();
    
    private PrintStream originalOut;
    private PrintStream originalErr;
    private boolean installed = false;
    
    /**
     * Install masking on System.out and System.err.
     * Safe to call multiple times; subsequent calls are ignored.
     */
    public synchronized StdIoMaskHelper install() {
        if (installed) {
            return this;
        }
        
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
     * Register a specific sensitive value to be masked in stdio output.
     * Delegates to LogMaskHelper with high sensitivity level.
     * 
     * @param sensitiveValue exact value to mask
     * @return this for method chaining
     */
    public synchronized StdIoMaskHelper registerValue(String sensitiveValue) {
        if (StringUtils.isNotBlank(sensitiveValue)) {
            LogMaskHelper.INSTANCE.registerStdioValue(LogSensitivityLevel.high, sensitiveValue, "<REDACTED>");
        }
        return this;
    }
    
    /**
     * Apply masking to stdio output using LogMaskHelper.
     * 
     * @param input text that may contain sensitive data
     * @return masked text with sensitive content replaced
     */
    private String mask(String input) {
        return LogMaskHelper.INSTANCE.maskStdio(input);
    }
    
    public synchronized boolean isInstalled() {
        return installed;
    }
}