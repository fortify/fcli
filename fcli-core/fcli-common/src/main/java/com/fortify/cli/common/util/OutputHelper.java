/**
 * Copyright 2023 Open Text.
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
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.io.output.NullOutputStream;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Helper class that allows for showing, collecting or suppressing output while running a given {@link Callable}.
 *
 * @author Ruud Senden
 */
@Builder
public class OutputHelper {
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final OutputType stdoutType;
    private final OutputType stderrType;
    @Builder.Default private final Charset charset = StandardCharsets.UTF_8;
    
    public final <T extends OutputStream> Result call(Callable<Integer> callable) throws Exception {
        var orgStdout = System.out;
        var orgStderr = System.err;
        try ( var stdoutStream = stdoutType.streamSupplier.apply(stdout);
              var stderrStream = stderrType.streamSupplier.apply(stderr);
              var stdoutPS = new PrintStream(stdoutStream);
              var stderrPS = new PrintStream(stderrStream) ) {
            System.setOut(stdoutPS);
            System.setErr(stderrPS);
            int exitCode = callable.call();
            System.out.flush();
            System.err.flush();
            return new Result(exitCode, stdoutType.stringFunction.apply(stdoutStream, charset), stderrType.stringFunction.apply(stderrStream, charset));
        } finally {
            System.setOut(orgStdout);
            System.setErr(orgStderr);
        }
    }
    
    @RequiredArgsConstructor
    public static enum OutputType {
        show(o->o, (s,c)->""), 
        collect(o->new ByteArrayOutputStream(), (s,c)->((ByteArrayOutputStream)s).toString(c)), 
        suppress(o->NullOutputStream.INSTANCE, (s,c)->"");
        
        private final Function<OutputStream,OutputStream> streamSupplier;
        private final BiFunction<OutputStream,Charset,String> stringFunction;
    }
    
    @Data @Reflectable
    public static final class Result {
        private final int exitCode;
        private final String out;
        private final String err;
    }
}
