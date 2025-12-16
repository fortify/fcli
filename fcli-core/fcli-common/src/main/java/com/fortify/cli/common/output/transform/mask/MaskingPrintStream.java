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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Function;

/**
 * PrintStream wrapper that masks sensitive content before delegating to the underlying stream.
 * Handles all text-based output methods (print, println, printf, format, write).
 */
public final class MaskingPrintStream extends PrintStream {
    private final PrintStream delegate;
    private final Function<String, String> masker;
    private final Charset charset;
    
    public MaskingPrintStream(PrintStream delegate, Function<String, String> masker) {
        super(new ByteArrayOutputStream());
        this.delegate = delegate;
        this.masker = masker;
        this.charset = StandardCharsets.UTF_8;
    }
    
    @Override
    public void print(String s) {
        delegate.print(masker.apply(s));
    }
    
    @Override
    public void println(String x) {
        delegate.println(masker.apply(x));
    }
    
    @Override
    public PrintStream printf(String format, Object... args) {
        var formatted = String.format(format, args);
        delegate.print(masker.apply(formatted));
        return this;
    }
    
    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        var formatted = String.format(l, format, args);
        delegate.print(masker.apply(formatted));
        return this;
    }
    
    @Override
    public void write(byte[] buf, int off, int len) {
        if (buf == null || len == 0) {
            return;
        }
        var text = new String(buf, off, len, charset);
        delegate.print(masker.apply(text));
    }
    
}