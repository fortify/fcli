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
package com.fortify.cli.common.output.transform.mask;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * PrintStream wrapper that masks sensitive content before delegating to the underlying stream.
 * Handles all text-based output methods (print, println, printf, format, write).
 * 
 * We create a custom OutputStream that wraps the original stream and applies masking,
 * then pass that to the PrintStream superclass. This avoids infinite recursion issues.
 */
public final class MaskingPrintStream extends PrintStream {
    
    public MaskingPrintStream(PrintStream delegate, Function<String, String> masker) {
        super(new MaskingOutputStream(delegate, masker), true);
    }
    
    /**
     * OutputStream that applies masking before writing to the delegate stream.
     */
    private static final class MaskingOutputStream extends OutputStream {
        private final PrintStream delegate;
        private final Function<String, String> masker;
        private final Charset charset = StandardCharsets.UTF_8;
        
        MaskingOutputStream(PrintStream delegate, Function<String, String> masker) {
            this.delegate = delegate;
            this.masker = masker;
        }
        
        @Override
        public void write(int b) {
            // For single bytes, just pass through without masking to avoid overhead
            // Masking is applied at the array level where we can process text
            delegate.write(b);
        }
        
        @Override
        public void write(byte[] buf, int off, int len) {
            if (buf == null || len == 0) {
                return;
            }
            var text = new String(buf, off, len, charset);
            var masked = masker.apply(text);
            var maskedBytes = masked.getBytes(charset);
            delegate.write(maskedBytes, 0, maskedBytes.length);
        }
        
        @Override
        public void flush() {
            delegate.flush();
        }
        
        @Override
        public void close() {
            delegate.close();
        }
    }
}