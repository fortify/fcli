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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.output.transform.mask.MaskingPrintStream;

import picocli.CommandLine.Help.Ansi;

/**
 * Central manager for fcli stdio delegation, masking, and progress streams.
 *
 * <p>On {@link #install()}, <em>System.out</em> and <em>System.err</em> are replaced
 * with {@link DelegatingPrintStream} instances that route to a per-thread stack of
 * PrintStreams, and {@link MaskingPrintStream} wrappers are pushed as the base layer
 * so that all output flowing through the stack is masked. Code can temporarily redirect
 * output via {@link #pushOut}/{@link #popOut} (and the corresponding err variants)
 * without affecting other threads.</p>
 *
 * <h3>Raw streams</h3>
 * <p>{@link #getRawOut()} / {@link #getRawErr()} return the unmasked
 * {@code System.out}/{@code System.err} captured <em>before</em> installation.
 * <strong>Only use for protocol I/O</strong> (e.g. JSON-RPC in RPC/MCP servers).
 * Writing arbitrary text to raw streams will bypass masking.</p>
 *
 * <h3>Progress streams</h3>
 * <p>{@link #getProgressOut()} / {@link #getProgressErr()} return masked streams
 * for progress/status output. They default to the masked originals, but can be
 * overridden via {@link #setProgressOut} / {@link #setProgressErr} (the provided
 * stream is auto-wrapped with masking; pass {@code null} to suppress output entirely).
 * RPC/MCP servers use these to redirect progress away from the JSON-RPC response channel.
 * Both setters must only be called from the install thread (before worker threads start).</p>
 *
 * @author Ruud Senden
 */
public final class StdioHelper {
    private static final Logger LOG = LoggerFactory.getLogger(StdioHelper.class);

    private StdioHelper() {}

    private static final ThreadLocal<Deque<PrintStream>> outStack = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<PrintStream>> errStack = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Consumer<String>> progressCallback = new ThreadLocal<>();

    private static volatile Thread installThread;
    private static volatile boolean installed = false;
    private static volatile Ansi ansi = Ansi.AUTO;
    private static PrintStream rawOut = System.out;
    private static PrintStream rawErr = System.err;
    private static PrintStream maskedOut = System.out;
    private static PrintStream maskedErr = System.err;
    private static volatile PrintStream progressOut = System.out;
    private static volatile PrintStream progressErr = System.err;

    /**
     * Install delegating streams, masking, and default progress streams.
     * Safe to call multiple times; subsequent calls are no-ops.
     */
    public static synchronized void install() {
        if ( installed ) return;
        // Detect ANSI capability before replacing streams: the delegating/masking
        // wrappers installed below can interfere with terminal-based ANSI probing.
        ansi = Ansi.AUTO.enabled() ? Ansi.ON : Ansi.OFF;
        rawOut = System.out;
        rawErr = System.err;
        installThread = Thread.currentThread();
        LOG.trace("Installing delegating streams; rawOut={}, rawErr={}",
                System.identityHashCode(rawOut), System.identityHashCode(rawErr));
        System.setOut(new DelegatingPrintStream(() -> {
            var stack = outStack.get();
            return stack.isEmpty() ? rawOut : stack.peek();
        }));
        System.setErr(new DelegatingPrintStream(() -> {
            var stack = errStack.get();
            return stack.isEmpty() ? rawErr : stack.peek();
        }));
        maskedOut = new MaskingPrintStream(rawOut, StdioHelper::mask);
        maskedErr = new MaskingPrintStream(rawErr, StdioHelper::mask);
        pushOut(maskedOut);
        pushErr(maskedErr);
        progressOut = maskedOut;
        progressErr = maskedErr;
        installed = true;
    }

    /**
     * Uninstall masking streams and restore the original {@code System.out}/
     * {@code System.err}. Safe to call multiple times; subsequent calls are no-ops.
     */
    public static synchronized void uninstall() {
        if ( !installed ) return;
        popOut();
        popErr();
        System.setOut(rawOut);
        System.setErr(rawErr);
        installed = false;
    }

    /**
     * Return the resolved ANSI mode, detected before streams were replaced.
     * Returns {@link Ansi#ON} if the terminal supports ANSI, {@link Ansi#OFF}
     * otherwise. Before {@link #install()} is called, returns {@link Ansi#AUTO}.
     */
    public static Ansi getAnsi() { return ansi; }

    /**
     * Return the raw, unmasked {@code System.out} captured before installation.
     * <p><strong>Only for protocol I/O</strong> (JSON-RPC in RPC/MCP servers).
     * All other code should use {@code System.out} or {@link #getProgressOut()}.</p>
     */
    public static PrintStream getRawOut() { return rawOut; }

    /**
     * Return the raw, unmasked {@code System.err} captured before installation.
     * <p><strong>Only for protocol I/O</strong> (JSON-RPC in RPC/MCP servers).
     * All other code should use {@code System.err} or {@link #getProgressErr()}.</p>
     */
    public static PrintStream getRawErr() { return rawErr; }

    /**
     * Return the masked stream designated for progress/status output.
     * Defaults to masked stdout unless overridden via {@link #setProgressOut}.
     */
    public static PrintStream getProgressOut() { return progressOut; }

    /**
     * Return the masked stream designated for progress/status error output.
     * Defaults to masked stderr unless overridden via {@link #setProgressErr}.
     */
    public static PrintStream getProgressErr() { return progressErr; }

    /**
     * Override the progress output stream (e.g. to redirect progress away from
     * the RPC channel). The provided stream is auto-wrapped with masking.
     * Pass {@code null} to suppress all progress output (e.g. for HTTP MCP servers).
     * <p><strong>Must only be called from the install thread</strong> (i.e. at startup,
     * before worker threads are spawned). Calling from any other thread throws
     * {@link FcliBugException}.</p>
     */
    public static void setProgressOut(PrintStream ps) {
        checkInstallThread("setProgressOut");
        LOG.trace("setProgressOut: {}", System.identityHashCode(ps));
        progressOut = wrapWithMasking(ps);
    }

    /**
     * Override the progress error stream (e.g. to redirect progress away from
     * the RPC channel). The provided stream is auto-wrapped with masking.
     * Pass {@code null} to suppress all progress error output (e.g. for HTTP MCP servers).
     * <p><strong>Must only be called from the install thread</strong> (i.e. at startup,
     * before worker threads are spawned). Calling from any other thread throws
     * {@link FcliBugException}.</p>
     */
    public static void setProgressErr(PrintStream ps) {
        checkInstallThread("setProgressErr");
        LOG.trace("setProgressErr: {}", System.identityHashCode(ps));
        progressErr = wrapWithMasking(ps);
    }

    /**
     * Set a per-thread callback invoked by progress writers on each
     * {@code writeProgress()} call. Used by the async job manager to forward
     * progress messages as structured notifications (e.g. JSON-RPC).
     * Masking is applied automatically before the callback is invoked.
     */
    public static void setProgressCallback(Consumer<String> callback) {
        progressCallback.set(msg -> callback.accept(mask(msg)));
    }

    /** Remove the per-thread progress callback. */
    public static void clearProgressCallback() {
        progressCallback.remove();
    }

    /** Return the per-thread progress callback, or {@code null} if none is set. */
    public static Consumer<String> getProgressCallback() {
        return progressCallback.get();
    }

    /** Return the current effective stdout for this thread (top of stack or raw). */
    public static PrintStream currentOut() {
        var stack = outStack.get();
        return stack.isEmpty() ? rawOut : stack.peek();
    }

    /** Return the current effective stderr for this thread (top of stack or raw). */
    public static PrintStream currentErr() {
        var stack = errStack.get();
        return stack.isEmpty() ? rawErr : stack.peek();
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

    private static void checkInstallThread(String method) {
        if (installThread != null && Thread.currentThread() != installThread) {
            throw new FcliBugException(method + " must only be called from the install thread");
        }
    }

    private static PrintStream wrapWithMasking(PrintStream ps) {
        if (ps == null) return new PrintStream(OutputStream.nullOutputStream());
        return ps instanceof MaskingPrintStream ? ps : new MaskingPrintStream(ps, StdioHelper::mask);
    }

    private static String mask(String input) {
        return LogMaskHelper.INSTANCE.maskStdio(input);
    }
}
