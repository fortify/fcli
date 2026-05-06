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
/*
 * Invocation-scoped output context providing thread-local PrintStream delegates
 */
package com.fortify.cli.common.cli.util;

import java.io.PrintStream;
import java.util.Objects;

public final class FcliExecutionOutputContext {
    private FcliExecutionOutputContext() {}

    private static final ThreadLocal<PrintStream> currentOut = new InheritableThreadLocal<>();
    private static final ThreadLocal<PrintStream> currentErr = new InheritableThreadLocal<>();

    private static volatile boolean installed = false;
    private static PrintStream originalOut;
    private static PrintStream originalErr;

    public static synchronized void installIfNeeded() {
        if ( installed ) return;
        // Capture originals before replacing
        originalOut = System.out;
        originalErr = System.err;
        // Install delegating streams that forward to the thread-local delegates or fall back to originals
        System.setOut(new DelegatingPrintStream(() -> Objects.requireNonNullElse(currentOut.get(), originalOut)));
        System.setErr(new DelegatingPrintStream(() -> Objects.requireNonNullElse(currentErr.get(), originalErr)));
        installed = true;
    }

    public static PrintStream getOriginalOut() { return originalOut; }
    public static PrintStream getOriginalErr() { return originalErr; }

    public static PrintStream getThreadOut() { return currentOut.get(); }
    public static PrintStream getThreadErr() { return currentErr.get(); }

    public static void setThreadOut(PrintStream ps) { currentOut.set(ps); }
    public static void setThreadErr(PrintStream ps) { currentErr.set(ps); }
    public static void clearThreadOut() { currentOut.remove(); }
    public static void clearThreadErr() { currentErr.remove(); }
}
