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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.fortify.cli.common.session.helper.ISessionDescriptor;

import lombok.Getter;

/**
 * Shared isolation boundary for request, auth, session, and cache scoped state.
 *
 * <p>Execution frames represented by {@link FcliExecutionContext} may be created
 * and discarded frequently, but related invocations can still share the same
 * isolation scope. This allows nested command invocations and background jobs to
 * resolve the same request/auth scoped caches and transient session descriptors
 * without reusing the same execution frame.</p>
 */
public final class FcliIsolationScope {
    @Getter private volatile String mcpRequestAuthScopeKey;
    @Getter private final Map<String, ISessionDescriptor> transientSessionDescriptors = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> scopedStates = new ConcurrentHashMap<>();

    public ISessionDescriptor getTransientSessionDescriptor(String type) {
        return type == null ? null : transientSessionDescriptors.get(type);
    }

    public void setTransientSessionDescriptor(ISessionDescriptor descriptor) {
        if ( descriptor != null ) {
            transientSessionDescriptors.put(descriptor.getType(), descriptor);
        }
    }

    public void clearTransientSessionDescriptor(String type) {
        if ( type != null ) {
            transientSessionDescriptors.remove(type);
        }
    }

    public void clearTransientSessionDescriptors() {
        transientSessionDescriptors.clear();
    }

    public void setMcpRequestAuthScopeKey(String mcpRequestAuthScopeKey) {
        this.mcpRequestAuthScopeKey = mcpRequestAuthScopeKey;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreateScopedState(Class<T> type, Supplier<T> supplier) {
        return (T)scopedStates.computeIfAbsent(type, ignored -> supplier.get());
    }
}
