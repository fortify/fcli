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
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

import lombok.Data;

public class OutputCollector {
    public static final Output collectOutput(Charset charset, Callable<Integer> callable) {
        var oldOut = System.out;
        var oldErr = System.err;
        try ( var outStream = new ByteArrayOutputStream();
              var errStream = new ByteArrayOutputStream();
              var outPS = new PrintStream(outStream);
              var errPS = new PrintStream(errStream) ) {
            System.setOut(outPS);
            System.setErr(errPS);
            int exitCode = callable.call();
            System.out.flush();
            System.err.flush();
            return new Output(exitCode, outStream.toString(charset), errStream.toString(charset));
        } catch ( Exception e ) {
            throw new RuntimeException("Error executing", e);
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }
    
    @Data
    public static final class Output {
        private final int exitCode;
        private final String out;
        private final String err;
    }
}
