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
package com.fortify.cli.util.rpc_server.helper.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.util.rpc_server.helper.rpc.RpcSessionManager.ProductType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC method handler for FoD session logout.
 * 
 * Method: fcli.fod.logout
 * Params: none required
 * 
 * Returns:
 *   - success (boolean): Whether logout was successful
 *   - sessionName (string): The session name that was logged out
 *   - product (string): "fod"
 *   - message (string): Status message
 *
 * @author Ruud Senden
 */
@Slf4j
@RequiredArgsConstructor
public final class RpcMethodHandlerFodLogout implements IRpcMethodHandler {
    private final ObjectMapper objectMapper;
    private final RpcSessionManager sessionManager;
    
    @Override
    public JsonNode execute(JsonNode params) throws RpcMethodException {
        log.debug("FoD logout");
        return sessionManager.executeLogout(ProductType.FOD);
    }
}
