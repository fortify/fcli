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
package com.fortify.cli.common.util;

import java.io.OutputStream;
import java.io.PrintStream;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NonClosingPrintStream extends PrintStream {
    private final boolean logDetailsonClose;
    private final String name;
    
    public NonClosingPrintStream(String name, OutputStream out) {
        this(true, name, out);
    }
    
    public NonClosingPrintStream(boolean logDetailsonClose, String name, OutputStream out) {
        super(out); 
        this.logDetailsonClose = logDetailsonClose;
        this.name = name;
    }
    
    @Override @SneakyThrows
    public final void close() {
        if ( logDetailsonClose ) {
            log.debug("{}.close() called - ignoring to avoid closing underlying stream", name, new RuntimeException());
        }
        log.debug("Flushing {}", name);
        out.flush();
        // Only flush, don't close underlying stream
    }
}