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

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;

/**
 * Mutable bag of {@code global.*} action variables shared by related action invocations.
 *
 * <p>Instances of this class are deliberately decoupled from {@link FcliExecutionContext}
 * so that the sharing rules for {@code global.*} variables can be configured independently
 * from the sharing rules for other per-execution resources:
 *
 * <ul>
 *   <li><b>External CLI invocation</b> — a fresh {@code FcliActionState} is created for every
 *       top-level command, so {@code global.*} variables cannot leak from one CLI call to the
 *       next.</li>
 *   <li><b>MCP / RPC tool call (non-imported)</b> — each tool call also gets a fresh
 *       {@code FcliActionState}, keeping calls independent.</li>
 *   <li><b>Imported action functions (MCP stdio / RPC stdio)</b> — all invocations within the same
 *       server instance share one {@code FcliActionState} instance. This is the mechanism that
 *       lets one exported function set a {@code global.*} variable that a subsequent call to a
 *       different exported function can read back.</li>
 *   <li><b>Imported action functions (MCP HTTP server)</b> — each distinct authenticated identity
 *       (credentials hash) has its own {@code FcliActionState}, scoped to the same
 *       {@link FcliIsolationScope} as its transient session descriptor. {@code global.*} variables
 *       therefore persist across calls from the same user but are never shared with other users.</li>
 *   <li><b>{@code run.fcli} sub-commands</b> — executed within the parent's existing
 *       {@link FcliExecutionContext}, so they see and can mutate the same
 *       {@code FcliActionState} as the calling action step.</li>
 * </ul>
 */
public final class FcliActionState {
    @Getter private final ObjectNode globalActionValues = new ObjectNode(JsonNodeFactory.instance, new ConcurrentHashMap<>());
}
