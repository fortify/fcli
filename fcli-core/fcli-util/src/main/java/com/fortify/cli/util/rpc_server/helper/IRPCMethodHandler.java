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
package com.fortify.cli.util.rpc_server.helper;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for JSON-RPC method handlers. Each handler is responsible for
 * executing a specific RPC method and returning the result.
 *
 * @author Ruud Senden
 */
public interface IRPCMethodHandler {
    /**
     * Execute the RPC method with the given parameters.
     *
     * @param params the method parameters (may be null)
     * @return the result as a JsonNode, or null if no result
     * @throws RPCMethodException if the method execution fails
     */
    JsonNode execute(JsonNode params) throws RPCMethodException;

    /** Short description shown by {@code rpc.listMethods}. */
    default String description() { return ""; }
}
