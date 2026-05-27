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

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.fortify.cli.common.session.helper.ISessionDescriptor;

import lombok.Getter;

/**
 * Shared isolation boundary grouping related invocations under the same auth/session context.
 *
 * <p>An isolation scope lives longer than a single {@link FcliExecutionContext}: many execution
 * frames can be created and destroyed while still referring to the same scope, allowing nested
 * commands and background jobs to resolve the same request-scoped caches and transient session
 * descriptors without sharing a single execution frame.</p>
 *
 * <p>Scope assignment per execution model:</p>
 * <ul>
 *   <li><b>Plain CLI command</b> — one brand-new scope per invocation; discarded when the
 *       command exits.</li>
 *   <li><b>MCP stdio server</b> — one scope for the entire server lifetime, shared by all
 *       tool calls. Every tool call gets its own {@link FcliExecutionContext} (fresh
 *       {@link com.fortify.cli.common.rest.unirest.UnirestContext}) but they all share the
 *       same transient sessions and scope-scoped state.</li>
 *   <li><b>MCP HTTP server</b> — one scope <em>per authenticated identity</em>. The HTTP
 *       transport carries credentials with every request; the server resolves the corresponding
 *       scope via {@code MCPServerHttpSessionDescriptorResolver}, so that two clients using
 *       different credentials are fully isolated from each other even when served by the same
 *       JVM.</li>
 *   <li><b>RPC server</b> — one scope for the entire server lifetime, similar to MCP stdio.</li>
 * </ul>
 */
public final class FcliIsolationScope {
    @Getter private volatile String mcpRequestAuthScopeKey;
    @Getter private volatile Path scopedVarsPath;
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

    public void setScopedVarsPath(Path scopedVarsPath) {
        this.scopedVarsPath = scopedVarsPath;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreateScopedState(Class<T> type, Supplier<T> supplier) {
        return (T)scopedStates.computeIfAbsent(type, ignored -> supplier.get());
    }
}
