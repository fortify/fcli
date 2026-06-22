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

import java.util.ArrayDeque;
import java.util.Deque;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

/**
 * Explicit holder for the current thread's execution context stack.
 * Use push()/pop() to manage nested execution contexts. No implicit
 * inheritance to child threads is performed; propagation must be explicit.
 *
 * <p>A context must always be pushed explicitly before any code that calls
 * {@link #current()} — typically at the entry point of each execution path
 * (plain CLI via {@link FcliExecutionStrategy}, MCP request handlers, RPC
 * request dispatch). Callers should never rely on automatic context creation.</p>
 *
 * <p>The preferred idiom for managing context lifetime is try-with-resources
 * using the {@link ContextFrame} returned by {@link #push} and {@link #pushNew}:</p>
 * <pre>{@code
 * try (var frame = FcliExecutionContextHolder.push(ctx)) {
 *     // use frame.context() if needed
 * } // automatically pops and closes
 * }</pre>
 */
public final class FcliExecutionContextHolder {
    private static final ThreadLocal<Deque<FcliExecutionContext>> HOLDER = ThreadLocal.withInitial(ArrayDeque::new);

    private FcliExecutionContextHolder() {}

    /**
     * Handle returned by {@link #push} and {@link #pushNew}.
     * Closing this frame pops the associated context from the stack and closes it,
     * releasing any resources it holds (e.g. cached Unirest connections).
     */
    public record ContextFrame(FcliExecutionContext context) implements AutoCloseable {
        @Override public void close() { pop(); }
    }

    /** Push the given context onto the current thread's context stack and return a closeable frame. */
    public static ContextFrame push(FcliExecutionContext ctx) {
        HOLDER.get().push(ctx);
        return new ContextFrame(ctx);
    }

    /**
     * Push a brand-new execution frame with its own isolation scope and action state.
     *
     * <p>Always creates a completely fresh {@link FcliExecutionContext} regardless of
     * whether a parent context is present on the stack. Use this at top-level entry points
     * (plain CLI via {@link FcliExecutionStrategy}, build-time actions) where no inherited
     * state is desired.</p>
     *
     * <p>Worker threads that need to share the parent's isolation scope should instead call
     * {@link #push(FcliExecutionContext)} with {@code parentContext.createChild()}.</p>
     */
    public static ContextFrame pushNew() {
        var stack = HOLDER.get();
        var context = new FcliExecutionContext();
        stack.push(context);
        return new ContextFrame(context);
    }

    /**
     * Pop the current context, close it, and return it.
     * Returns {@code null} if no context is present.
     */
    public static FcliExecutionContext pop() {
        var stack = HOLDER.get();
        if ( stack.isEmpty() ) { return null; }
        var result = stack.pop();
        if ( stack.isEmpty() ) { HOLDER.remove(); }
        result.close();
        return result;
    }

    /**
     * Return the current (top) context.
     *
     * @throws IllegalStateException if no context has been pushed on the current thread,
     *         which indicates a missing push at an execution entry point.
     */
    public static FcliExecutionContext current() { 
        var stack = HOLDER.get();
        if ( stack.isEmpty() ) {
            throw new FcliBugException(
                "No FcliExecutionContext on the current thread. "
                + "Ensure a context is pushed at every execution entry point "
                + "(CLI command, MCP request, RPC request).");
        }
        return stack.peek(); 
    }

    /**
     * Look up a transient session descriptor by type from the current context's
     * isolation scope. Since child contexts created via
     * {@link FcliExecutionContext#createChild()} share the same
     * {@link FcliIsolationScope} reference as their parent, transient session
     * descriptors registered in the parent are visible to all children without
     * requiring stack traversal.
     */
    public static ISessionDescriptor getTransientSessionDescriptor(String type) {
        return current().getIsolationScope().getTransientSessionDescriptor(type);
    }

    public static String getMcpRequestAuthScopeKey() {
        return current().getIsolationScope().getMcpRequestAuthScopeKey();
    }

    /**
     * Return the current (top) execution context, or {@code null} if no context is pushed.
     * Used by {@link com.fortify.cli.common.log.LogMaskContext#activeContext()} for
     * per-request log masking; must not throw.
     */
    public static FcliExecutionContext tryCurrentContext() {
        var stack = HOLDER.get();
        return stack.isEmpty() ? null : stack.peek();
    }
    
    /** Return the current stack depth. Useful for logging/troubleshooting. */
    public static int stackDepth() { return HOLDER.get().size(); }
}
