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
package com.fortify.cli.common.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import org.apache.commons.io.output.NullOutputStream;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.cli.util.StdioHelper;

import lombok.Builder;
import lombok.Data;

/**
 * Helper class that allows for showing, collecting or suppressing output while running a given {@link Callable}.
 *
 * @author Ruud Senden
 */
@Builder
public class OutputHelper {
    private final OutputType stdoutType;
    private final OutputType stderrType;
    @Builder.Default private final Charset charset = StandardCharsets.UTF_8;
    
    public final Result call(Callable<Integer> callable) throws Exception {
        var stdoutPS = stdoutType.createStream();
        var stderrPS = stderrType.createStream();
        if (stdoutPS != null) StdioHelper.pushOut(stdoutPS);
        if (stderrPS != null) StdioHelper.pushErr(stderrPS);
        try {
            int exitCode = callable.call();
            System.out.flush();
            System.err.flush();
            return new Result(exitCode, getCollectedString(stdoutPS), getCollectedString(stderrPS));
        } finally {
            if (stderrPS != null) { StdioHelper.popErr(); stderrPS.close(); }
            if (stdoutPS != null) { StdioHelper.popOut(); stdoutPS.close(); }
        }
    }
    
    private String getCollectedString(PrintStream stream) {
        return stream instanceof CollectingPrintStream cps ? cps.getOutput(charset) : "";
    }
    
    public static enum OutputType {
        show, collect, suppress;
        
        PrintStream createStream() {
            return switch (this) {
                case show -> null;
                case collect -> new CollectingPrintStream();
                case suppress -> new PrintStream(NullOutputStream.INSTANCE);
            };
        }
    }
    
    private static final class CollectingPrintStream extends PrintStream {
        CollectingPrintStream() {
            super(new ByteArrayOutputStream());
        }
        
        String getOutput(Charset charset) {
            flush();
            return ((ByteArrayOutputStream) out).toString(charset);
        }
    }
    
    @Data @Reflectable
    public static final class Result {
        private final int exitCode;
        private final String out;
        private final String err;
    }
}
