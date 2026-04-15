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

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invocation-scoped output context providing thread-local {@link PrintStream} delegates.
 *
 * <p>On first {@link #installIfNeeded()} call, <em>System.out</em> and <em>System.err</em>
 * are replaced with {@link DelegatingPrintStream} instances that route to a per-thread
 * stack of PrintStreams. Code can temporarily redirect output via {@link #pushOut}/{@link #popOut}
 * (and the corresponding err variants) without affecting other threads.</p>
 *
 * <h3>Original streams</h3>
 * <p>{@link #getOriginalOut()} / {@link #getOriginalErr()} return the raw
 * {@code System.out}/{@code System.err} captured <em>before</em> any delegation was installed.
 * <strong>Use with care:</strong> in RPC/MCP server contexts, the original stdout is the
 * JSON-RPC stdio channel. Writing arbitrary text to it will corrupt the protocol framing.
 * Prefer {@link #getProgressOut()} / {@link #getProgressErr()} for user-facing messages such
 * as progress output, or use the per-thread delegation stack via {@code System.out} directly.</p>
 *
 * <h3>Progress streams</h3>
 * <p>{@link #getProgressOut()} / {@link #getProgressErr()} default to the original streams,
 * but can be overridden via {@link #setProgressOut} / {@link #setProgressErr} to redirect
 * progress messages. The RPC & MCP servers set these to avoid progress output from interfering
 * with the JSON-RPC response channel.</p>
 *
 * @author Ruud Senden
 */
public final class FcliExecutionOutputContext {
    private static final Logger LOG = LoggerFactory.getLogger(FcliExecutionOutputContext.class);

    private FcliExecutionOutputContext() {}

    private static final ThreadLocal<Deque<PrintStream>> outStack = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<PrintStream>> errStack = ThreadLocal.withInitial(ArrayDeque::new);

    private static volatile boolean installed = false;
    private static PrintStream originalOut;
    private static PrintStream originalErr;
    private static volatile PrintStream progressOut;
    private static volatile PrintStream progressErr;

    /**
     * Install delegating streams on {@code System.out} and {@code System.err} if not
     * already installed. Safe to call multiple times; subsequent calls are no-ops.
     */
    public static synchronized void installIfNeeded() {
        if ( installed ) return;
        originalOut = System.out;
        originalErr = System.err;
        LOG.trace("Installing delegating streams; originalOut={}, originalErr={}",
                System.identityHashCode(originalOut), System.identityHashCode(originalErr));
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

    /**
     * Return the raw {@code System.out} captured before delegation was installed.
     * <p><strong>Caution:</strong> in RPC/MCP server contexts this is the JSON-RPC stdio
     * channel. Writing arbitrary text to it will corrupt protocol framing. Prefer
     * {@link #getProgressOut()} for progress/status messages.</p>
     */
    public static PrintStream getOriginalOut() { return originalOut; }

    /**
     * Return the raw {@code System.err} captured before delegation was installed.
     * <p><strong>Caution:</strong> in RPC/MCP server contexts this may be the server's
     * status channel. Prefer {@link #getProgressErr()} for progress/status messages.</p>
     */
    public static PrintStream getOriginalErr() { return originalErr; }

    /**
     * Return the stream designated for progress/status output. Defaults to
     * {@link #getOriginalOut()} unless overridden via {@link #setProgressOut}.
     */
    public static PrintStream getProgressOut() { return progressOut != null ? progressOut : originalOut; }

    /**
     * Return the stream designated for progress/status error output. Defaults to
     * {@link #getOriginalErr()} unless overridden via {@link #setProgressErr}.
     */
    public static PrintStream getProgressErr() { return progressErr != null ? progressErr : originalErr; }

    /** Override the progress output stream (e.g. to redirect progress away from the RPC channel). */
    public static void setProgressOut(PrintStream ps) {
        LOG.trace("setProgressOut: {}", System.identityHashCode(ps));
        progressOut = ps;
    }

    /** Override the progress error stream (e.g. to redirect progress away from the RPC channel). */
    public static void setProgressErr(PrintStream ps) {
        LOG.trace("setProgressErr: {}", System.identityHashCode(ps));
        progressErr = ps;
    }

    /** Return the current effective stdout for this thread (top of stack or original). */
    public static PrintStream currentOut() {
        var stack = outStack.get();
        return stack.isEmpty() ? originalOut : stack.peek();
    }

    /** Return the current effective stderr for this thread (top of stack or original). */
    public static PrintStream currentErr() {
        var stack = errStack.get();
        return stack.isEmpty() ? originalErr : stack.peek();
    }

    /** Push a new stdout redirect onto this thread's stack. */
    public static void pushOut(PrintStream ps) {
        var stack = outStack.get();
        LOG.trace("pushOut: {} (depth {} -> {})", System.identityHashCode(ps), stack.size(), stack.size() + 1);
        stack.push(ps);
    }

    /** Push a new stderr redirect onto this thread's stack. */
    public static void pushErr(PrintStream ps) {
        var stack = errStack.get();
        LOG.trace("pushErr: {} (depth {} -> {})", System.identityHashCode(ps), stack.size(), stack.size() + 1);
        stack.push(ps);
    }

    /** Pop the most recent stdout redirect from this thread's stack and return it. */
    public static PrintStream popOut() {
        var stack = outStack.get();
        var ps = stack.pop();
        LOG.trace("popOut: {} (depth {} -> {})", System.identityHashCode(ps), stack.size() + 1, stack.size());
        return ps;
    }

    /** Pop the most recent stderr redirect from this thread's stack and return it. */
    public static PrintStream popErr() {
        var stack = errStack.get();
        var ps = stack.pop();
        LOG.trace("popErr: {} (depth {} -> {})", System.identityHashCode(ps), stack.size() + 1, stack.size());
        return ps;
    }
}
