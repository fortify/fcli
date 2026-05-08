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
 */
public final class FcliExecutionContextHolder {
    private static final ThreadLocal<Deque<FcliExecutionContext>> HOLDER = ThreadLocal.withInitial(ArrayDeque::new);

    private FcliExecutionContextHolder() {}

    /** Push the given context onto the current thread's context stack. */
    public static void push(FcliExecutionContext ctx) { HOLDER.get().push(ctx); }

    /** Push a fresh, empty context and return it. */
    public static FcliExecutionContext pushNew() { 
        HOLDER.get().push(new FcliExecutionContext()); 
        return HOLDER.get().peek(); 
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
     * Return the current (top) context. If none is present a default
     * top-level context is created and pushed so this method never returns
     * null and callers may safely assume a non-null result.
     */
    public static FcliExecutionContext current() { 
        var stack = HOLDER.get(); 
        if ( stack.isEmpty() ) { stack.push(new FcliExecutionContext()); } 
        return stack.peek(); 
    }

    /**
     * Look up a transient session descriptor by type, searching from top to bottom
     * through the current thread's execution-context stack.
     */
    public static ISessionDescriptor getTransientSessionDescriptor(String type) {
        for ( var context : HOLDER.get() ) {
            var descriptor = context.getTransientSessionDescriptor(type);
            if ( descriptor != null ) {
                return descriptor;
            }
        }
        return null;
    }
    
    /** Return the current stack depth. Useful for logging/troubleshooting. */
    public static int stackDepth() { return HOLDER.get().size(); }
}
