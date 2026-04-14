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
import java.util.ArrayDeque;
import java.util.Deque;

public final class FcliExecutionOutputContext {
    private FcliExecutionOutputContext() {}

    private static final ThreadLocal<Deque<PrintStream>> outStack = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<PrintStream>> errStack = ThreadLocal.withInitial(ArrayDeque::new);

    private static volatile boolean installed = false;
    private static PrintStream originalOut;
    private static PrintStream originalErr;

    public static synchronized void installIfNeeded() {
        if ( installed ) return;
        // Capture originals before replacing
        originalOut = System.out;
        originalErr = System.err;
        // Install delegating streams that peek the per-thread stack or fall back to originals
        System.setOut(new DelegatingPrintStream(() -> {
            var stack = outStack.get();
            return stack.isEmpty() ? originalOut : stack.peek();
        }));
        System.setErr(new DelegatingPrintStream(() -> {
            var stack = errStack.get();
            return stack.isEmpty() ? originalErr : stack.peek();
        }));
        installed = true;
    }

    public static PrintStream getOriginalOut() { return originalOut; }
    public static PrintStream getOriginalErr() { return originalErr; }

    public static void pushOut(PrintStream ps) { outStack.get().push(ps); }
    public static void pushErr(PrintStream ps) { errStack.get().push(ps); }
    public static PrintStream popOut() { return outStack.get().pop(); }
    public static PrintStream popErr() { return errStack.get().pop(); }
}
