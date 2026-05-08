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
 */
public final class FcliExecutionContextHolder {
    private static final ThreadLocal<Deque<FcliExecutionContext>> HOLDER = ThreadLocal.withInitial(ArrayDeque::new);

    private FcliExecutionContextHolder() {}

    /** Push the given context onto the current thread's context stack. */
    public static void push(FcliExecutionContext ctx) { HOLDER.get().push(ctx); }

    /**
     * Push a fresh execution frame, inheriting the current isolation scope when
     * a parent context is present so nested invocations remain within the same
     * isolation boundary while still receiving a fresh action state.
     */
    public static FcliExecutionContext pushNew() { 
        var stack = HOLDER.get();
        var context = stack.isEmpty() ? new FcliExecutionContext() : stack.peek().createChild();
        stack.push(context);
        return context; 
    }

    /** Pop the current context and return it; returns null if none present. */
    public static FcliExecutionContext pop() {
        var stack = HOLDER.get();
        if ( stack.isEmpty() ) { return null; }
        var result = stack.pop();
        if ( stack.isEmpty() ) { HOLDER.remove(); }
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
            throw new IllegalStateException(
                "No FcliExecutionContext on the current thread. "
                + "Ensure a context is pushed at every execution entry point "
                + "(CLI command, MCP request, RPC request).");
        }
        return stack.peek(); 
    }

    /**
     * Look up a transient session descriptor by type, searching from top to bottom
     * through the current thread's execution-context stack.
     */
    public static ISessionDescriptor getTransientSessionDescriptor(String type) {
        return current().getIsolationScope().getTransientSessionDescriptor(type);
    }

    public static String getMcpRequestAuthScopeKey() {
        return current().getIsolationScope().getMcpRequestAuthScopeKey();
    }
    
    /** Return the current stack depth. Useful for logging/troubleshooting. */
    public static int stackDepth() { return HOLDER.get().size(); }
}
