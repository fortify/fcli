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
package com.fortify.cli.common.cli.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * PrintStream that delegates all write/flush calls to a supplied per-thread PrintStream.
 */
public final class DelegatingPrintStream extends PrintStream {
    private final Supplier<PrintStream> delegateSupplier;

    public DelegatingPrintStream(Supplier<PrintStream> delegateSupplier) {
        super(new OutputStream(){ @Override public void write(int b) {} });
        this.delegateSupplier = delegateSupplier;
    }

    private PrintStream delegate() {
        PrintStream ps = delegateSupplier.get();
        if ( ps!=null ) return ps;
        // Fallback: wrap the underlying OutputStream of this PrintStream
        return new PrintStream(super.out);
    }

    @Override public void write(int b) { delegate().write(b); }
    @Override public void write(byte[] buf, int off, int len) { delegate().write(buf, off, len); }
    @Override public void flush() { delegate().flush(); }
    @Override public void close() { /* avoid closing underlying streams from here */ delegate().flush(); }
}
